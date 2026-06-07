package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.command.product.CreateProductCommand.AttrCmd;
import com.hieu.catalog_service.application.command.variant.AddVariantCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.common.ValidationException;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.exception.AttrValNotFoundException;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.exception.VariantSkuAlreadyExistsException;
import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.VariantAttr;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddVariantHandler implements CommandHandler<AddVariantCommand, VariantDTO> {

    private final ProductRepository productRepository;
    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public VariantDTO handle(AddVariantCommand cmd) {
        var product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
            .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));

        var sku = Sku.of(cmd.sku());
        if (productRepository.existsBySku(sku)) throw new VariantSkuAlreadyExistsException(sku.value());

        var variant = Variant.create(product.getId(), sku,
            Money.of(cmd.price()), Money.ofNullable(cmd.cost()), Money.ofNullable(cmd.salePrice()),
            cmd.image(), cmd.weight(), Quantity.of(cmd.quantity()));

        Optional.ofNullable(cmd.attrs()).orElse(List.of())
            .forEach(ac -> variant.addAttr(resolveAttr(ac)));

        product.addVariant(variant);
        var saved = productRepository.save(product);
        eventPublisher.publishEventsOf(saved);

        return saved.getVariants().stream()
            .filter(v -> v.getSku().equals(sku))
            .findFirst()
            .map(mapper::toDto)
            .orElseThrow();
    }

    private VariantAttr resolveAttr(AttrCmd ac) {
        Attr attr = attrRepository.findById(AttrId.of(ac.attrId()))
            .orElseThrow(() -> new AttrNotFoundException(ac.attrId()));
        if (attr.getType() == AttrType.SELECT) {
            if (ac.attrValId() == null) throw new ValidationException(
                "Attribute " + attr.getCode() + " is SELECT — attrValId is required");
            var val = attr.getValues().stream()
                .filter(v -> v.getId().equals(ac.attrValId()))
                .findFirst()
                .orElseThrow(() -> new AttrValNotFoundException(ac.attrValId()));
            return VariantAttr.create(attr.getId(), attr.getCode(), attr.getName(), val.getId(), val.getVal());
        }
        if (ac.valText() == null || ac.valText().isBlank()) throw new ValidationException(
            "Attribute " + attr.getCode() + " is " + attr.getType() + " — valText is required");
        return VariantAttr.create(attr.getId(), attr.getCode(), attr.getName(), null, ac.valText().trim());
    }
}
