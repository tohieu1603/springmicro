package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fired after a product is persisted with its initial variants. Consumers: inventory
 * (snapshot initial stock), search (index), analytics (kick off lifetime metrics).
 */
@Getter
public final class ProductCreatedEvent extends DomainEvent {

    private final String productId;
    private final String name;
    private final String slug;
    private final String description;
    private final String brand;
    private final String status;
    private final String thumbnail;
    private final String categoryId;
    private final String createdBy;
    private final List<VariantInfo> variants;

    public ProductCreatedEvent(String productId, String name, String slug,
                               String description, String brand, String status, String thumbnail,
                               String categoryId,
                               String createdBy, List<VariantInfo> variants) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.name = Objects.requireNonNull(name, "name");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.description = description;
        this.brand = brand;
        this.status = status;
        this.thumbnail = thumbnail;
        this.categoryId = categoryId;
        this.createdBy = createdBy;
        this.variants = List.copyOf(Objects.requireNonNullElse(variants, List.of()));
    }

    @Override public String aggregateId() { return productId; }

    /** Flat projection of a variant — only what downstream consumers need. */
    public record VariantInfo(String variantId, String sku, BigDecimal price, int quantity) {}
}
