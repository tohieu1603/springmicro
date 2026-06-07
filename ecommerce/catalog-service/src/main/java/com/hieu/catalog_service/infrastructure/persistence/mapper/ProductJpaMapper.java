package com.hieu.catalog_service.infrastructure.persistence.mapper;

import tools.jackson.core.type.TypeReference;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.VariantAttr;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.Slug;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantStatus;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.ProductJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.VariantAttrJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.VariantJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product aggregate ↔ JPA entity mapping.
 *
 * <p>Images are serialised as JSON array text — a single {@code TEXT} column beats a
 * separate join table for the small galleries we expect (≤ 20 URLs). Variants and their
 * attrs are reconciled in-place by id so Hibernate fires the right INSERT / UPDATE /
 * DELETE against the managed collection without dropping the whole variants row set.
 */
@Component
@RequiredArgsConstructor
public class ProductJpaMapper {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ProductJpaEntity toJpa(Product p, ProductJpaEntity existing) {
        ProductJpaEntity e = existing != null ? existing : new ProductJpaEntity();
        e.setName(p.getName());
        e.setSlug(p.getSlug().value());
        e.setDescription(p.getDescription());
        e.setCategoryId(p.getCategoryId() != null ? p.getCategoryId().value() : null);
        e.setBrand(p.getBrand());
        e.setThumbnail(p.getThumbnail());
        e.setImages(writeJson(p.getImages()));
        e.setStatus(p.getStatus().name());
        e.setMetaTitle(p.getMetaTitle());
        e.setMetaDescription(p.getMetaDescription());
        e.setMetaKeywords(p.getMetaKeywords());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setCreatedBy(p.getCreatedBy());
        e.setUpdatedBy(p.getUpdatedBy());
        reconcileVariants(e, p.getVariants());
        return e;
    }

    public Product toDomain(ProductJpaEntity e) {
        List<Variant> variants = e.getVariants().stream()
            .map(this::toDomainVariant)
            .toList();
        return Product.reconstitute(
            ProductId.of(e.getId()),
            e.getName(),
            Slug.of(e.getSlug()),
            e.getDescription(),
            e.getCategoryId() != null ? CategoryId.of(e.getCategoryId()) : null,
            e.getBrand(),
            e.getThumbnail(),
            readJson(e.getImages()),
            ProductStatus.fromString(e.getStatus()),
            e.getMetaTitle(), e.getMetaDescription(), e.getMetaKeywords(),
            e.getCreatedAt(), e.getUpdatedAt(),
            e.getCreatedBy(), e.getUpdatedBy(), e.getVersion(),
            variants
        );
    }

    /**
     * After {@code save()} returns the persisted entity, pull the assigned ids back onto
     * the domain aggregate so downstream event-raising and the DTO response see them.
     */
    public void syncGeneratedIds(Product aggregate, ProductJpaEntity saved) {
        if (aggregate.getId() == null) aggregate.assignId(ProductId.of(saved.getId()));
        Map<String, VariantJpaEntity> entitiesBySku = new HashMap<>();
        saved.getVariants().forEach(v -> entitiesBySku.put(v.getSku(), v));

        for (Variant v : aggregate.getVariants()) {
            VariantJpaEntity entity = entitiesBySku.get(v.getSku().value());
            if (entity == null) continue;
            if (v.getId() == null) v.assignId(VariantId.of(entity.getId()));
            v.assignProductId(ProductId.of(saved.getId()));
            Map<String, String> attrIdByCode = new HashMap<>();
            entity.getAttrs().forEach(a -> attrIdByCode.put(a.getAttrCode(), a.getId()));
            v.getAttrs().forEach(va -> {
                if (va.getId() == null) {
                    String id = attrIdByCode.get(va.getAttrCode());
                    if (id != null) va.assignId(id);
                }
            });
        }
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void reconcileVariants(ProductJpaEntity productEntity, List<Variant> domain) {
        Map<String, VariantJpaEntity> byId = new HashMap<>();
        productEntity.getVariants().forEach(v -> { if (v.getId() != null) byId.put(v.getId(), v); });

        List<VariantJpaEntity> target = new ArrayList<>();
        for (Variant v : domain) {
            VariantJpaEntity ve = v.getId() != null ? byId.get(v.getId().value()) : null;
            if (ve == null) {
                ve = new VariantJpaEntity();
                ve.setProduct(productEntity);
            }
            ve.setSku(v.getSku().value());
            ve.setPrice(v.getPrice().amount());
            ve.setCost(v.getCost() != null ? v.getCost().amount() : null);
            ve.setSalePrice(v.getSalePrice() != null ? v.getSalePrice().amount() : null);
            ve.setImage(v.getImage());
            ve.setWeight(v.getWeight());
            ve.setQuantity(v.getQuantity().value());
            ve.setStatus(v.getStatus().name());
            reconcileVariantAttrs(ve, v.getAttrs());
            target.add(ve);
        }
        productEntity.getVariants().clear();
        productEntity.getVariants().addAll(target);
    }

    private void reconcileVariantAttrs(VariantJpaEntity variantEntity, List<VariantAttr> domain) {
        Map<String, VariantAttrJpaEntity> byId = new HashMap<>();
        variantEntity.getAttrs().forEach(a -> { if (a.getId() != null) byId.put(a.getId(), a); });

        List<VariantAttrJpaEntity> target = new ArrayList<>();
        for (VariantAttr va : domain) {
            VariantAttrJpaEntity ae = va.getId() != null ? byId.get(va.getId()) : null;
            if (ae == null) {
                ae = new VariantAttrJpaEntity();
                ae.setVariant(variantEntity);
            }
            ae.setAttrId(va.getAttrId().value());
            ae.setAttrCode(va.getAttrCode());
            ae.setAttrName(va.getAttrName());
            ae.setValId(va.getValId());
            ae.setValText(va.getValText());
            target.add(ae);
        }
        variantEntity.getAttrs().clear();
        variantEntity.getAttrs().addAll(target);
    }

    private Variant toDomainVariant(VariantJpaEntity e) {
        List<VariantAttr> attrs = e.getAttrs().stream()
            .map(a -> VariantAttr.reconstitute(a.getId(), AttrId.of(a.getAttrId()),
                a.getAttrCode(), a.getAttrName(), a.getValId(), a.getValText()))
            .toList();
        return Variant.reconstitute(
            VariantId.of(e.getId()),
            ProductId.of(e.getProduct().getId()),
            Sku.of(e.getSku()),
            Money.of(e.getPrice()),
            Money.ofNullable(e.getCost()),
            Money.ofNullable(e.getSalePrice()),
            e.getImage(),
            e.getWeight(),
            Quantity.of(e.getQuantity()),
            VariantStatus.fromString(e.getStatus()),
            e.getVersion(),
            attrs
        );
    }

    private String writeJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialise images", ex);
        }
    }

    private List<String> readJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to deserialise images: " + json, ex);
        }
    }
}
