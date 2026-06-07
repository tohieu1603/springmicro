package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.CreateProductCommand;
import com.hieu.catalog_service.application.command.product.CreateProductCommand.AttrCmd;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.common.ValidationException;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.exception.AttrValNotFoundException;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.exception.ProductAlreadyExistsException;
import com.hieu.catalog_service.domain.exception.VariantSkuAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.VariantAttr;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.Slug;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates a product with its initial variants in one transaction, denormalising attributes
 * so variant rows carry {@code attrCode/attrName + value} (no extra join for detail pages).
 * Emits {@code ProductCreatedEvent} AFTER_COMMIT so inventory-service can snapshot stock.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductHandler implements CommandHandler<CreateProductCommand, ProductDTO> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public ProductDTO handle(CreateProductCommand cmd) {
        validate(cmd);
        try {
            return doCreate(cmd);
        } catch (DataIntegrityViolationException e) {
            // existsBySku + save is not atomic — two concurrent creates with the same SKU
            // both pass the check, then DB unique constraint catches the loser. Translate
            // that into a clean domain exception instead of leaking the raw JPA error.
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("sku")) {
                throw new VariantSkuAlreadyExistsException("SKU already exists in product '" + cmd.name() + "'");
            }
            if (msg.contains("slug")) {
                throw new ProductAlreadyExistsException("Slug already exists: " + cmd.name());
            }
            throw e;
        }
    }

    private ProductDTO doCreate(CreateProductCommand cmd) {
        Optional.ofNullable(cmd.categoryId())
            .map(CategoryId::of)
            .filter(id -> !categoryRepository.existsById(id))
            .ifPresent(id -> { throw new CategoryNotFoundException(id.value()); });

        Map<AttrId, Attr> attrs = loadAttrs(cmd.variants());

        var product = Product.create(
            cmd.name(), cmd.description(),
            Optional.ofNullable(cmd.categoryId()).map(CategoryId::of).orElse(null),
            cmd.brand(), cmd.createdBy());
        product.replaceSlug(ensureUniqueSlug(Slug.generate(cmd.name())));
        if (cmd.thumbnail() != null || cmd.images() != null) {
            product.updateImages(cmd.thumbnail(), cmd.images(), cmd.createdBy());
        }
        if (cmd.metaTitle() != null || cmd.metaDescription() != null || cmd.metaKeywords() != null) {
            product.updateSeo(cmd.metaTitle(), cmd.metaDescription(), cmd.metaKeywords(), cmd.createdBy());
        }
        // Variants MUST be added before activation — Product.activate() rejects empty variants.
        cmd.variants().forEach(vc -> product.addVariant(buildVariant(vc, attrs)));
        if (cmd.activate()) product.activate(cmd.createdBy());

        var saved = productRepository.save(product);
        saved.raiseCreatedEvent();
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }

    private Variant buildVariant(CreateProductCommand.VariantCmd vc, Map<AttrId, Attr> attrs) {
        var sku = Sku.of(vc.sku());
        if (productRepository.existsBySku(sku)) throw new VariantSkuAlreadyExistsException(sku.value());
        var variant = Variant.create(sku,
            Money.of(vc.price()), Money.ofNullable(vc.cost()), Money.ofNullable(vc.salePrice()),
            vc.image(), vc.weight(), Quantity.of(vc.quantity()));
        Optional.ofNullable(vc.attrs()).orElse(List.of()).stream()
            .map(ac -> buildVariantAttr(attrs.get(AttrId.of(ac.attrId())), ac))
            .forEach(variant::addAttr);
        return variant;
    }

    private VariantAttr buildVariantAttr(Attr attr, AttrCmd ac) {
        return switch (attr.getType()) {
            case SELECT -> {
                if (ac.attrValId() == null) throw new ValidationException(
                    "Attribute " + attr.getCode() + " is SELECT — attrValId is required");
                var val = attr.getValues().stream()
                    .filter(v -> v.getId().equals(ac.attrValId()))
                    .findFirst()
                    .orElseThrow(() -> new AttrValNotFoundException(ac.attrValId()));
                yield VariantAttr.create(attr.getId(), attr.getCode(), attr.getName(), val.getId(), val.getVal());
            }
            case TEXT, NUMBER -> {
                if (ac.valText() == null || ac.valText().isBlank()) throw new ValidationException(
                    "Attribute " + attr.getCode() + " is " + attr.getType() + " — valText is required");
                yield VariantAttr.create(attr.getId(), attr.getCode(), attr.getName(), null, ac.valText().trim());
            }
        };
    }

    private Map<AttrId, Attr> loadAttrs(List<CreateProductCommand.VariantCmd> variants) {
        // Batch fetch — previous version did N round-trips through findById for each
        // distinct attrId. With 3 variants × 4 attrs = 12 SELECTs collapsed to one.
        var ids = variants.stream()
            .flatMap(v -> Optional.ofNullable(v.attrs()).orElse(List.of()).stream())
            .map(AttrCmd::attrId)
            .distinct()
            .toList();
        if (ids.isEmpty()) return Map.of();
        var loaded = attrRepository.findAllByIdsWithValues(ids).stream()
            .collect(Collectors.toMap(Attr::getId, Function.identity()));
        // Catch unknown ids — pinpoint the missing one in the error.
        ids.stream()
            .filter(id -> !loaded.containsKey(AttrId.of(id)))
            .findFirst()
            .ifPresent(id -> { throw new AttrNotFoundException(id); });
        return loaded;
    }

    private Slug ensureUniqueSlug(Slug base) {
        if (!productRepository.existsBySlug(base)) return base;
        // currentTimeMillis collides under burst — UUID suffix has near-zero collision
        // probability and still leaves the slug readable. The outer handle() also catches
        // the residual DB-unique race and maps it to ProductAlreadyExistsException.
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String candidate = base.value() + "-" + suffix;
        var next = Slug.of(candidate.length() > 128 ? candidate.substring(0, 128) : candidate);
        if (productRepository.existsBySlug(next)) throw new ProductAlreadyExistsException(base.value());
        return next;
    }

    private void validate(CreateProductCommand cmd) {
        var errors = new LinkedHashMap<String, String>();
        if (cmd.name() == null || cmd.name().isBlank()) errors.put("name", "required");
        if (cmd.variants() == null || cmd.variants().isEmpty()) {
            errors.put("variants", "at least one variant is required");
        }
        if (!errors.isEmpty()) throw new ValidationException("Invalid product payload", errors);
    }
}
