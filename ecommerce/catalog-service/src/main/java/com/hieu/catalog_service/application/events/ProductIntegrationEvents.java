package com.hieu.catalog_service.application.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JSON-serialisable integration events for the product/variant family. Separate from
 * domain events (in {@code com.hieu.catalog_service.domain.events.product}) because
 * integration events are part of the public contract with downstream services and
 * should never leak aggregate-internal VOs.
 */
public final class ProductIntegrationEvents {

    private ProductIntegrationEvents() {}

    public record ProductCreated(
            UUID eventId,
            Instant occurredOn,
            String productId,
            String name,
            String slug,
            String description,
            String brand,
            String status,
            String thumbnail,
            String categoryId,
            String createdBy,
            List<VariantSnapshot> variants
    ) {}

    public record ProductUpdated(
            UUID eventId, Instant occurredOn,
            String productId, String name, String slug, String updatedBy
    ) {}

    public record ProductStatusChanged(
            UUID eventId, Instant occurredOn,
            String productId, String oldStatus, String newStatus, String updatedBy
    ) {}

    public record ProductDeleted(
            UUID eventId, Instant occurredOn,
            String productId, List<String> variantIds, String deletedBy
    ) {}

    public record VariantAdded(
            UUID eventId, Instant occurredOn,
            String productId, String variantId, String sku, BigDecimal price, int quantity, String createdBy
    ) {}

    public record VariantRemoved(
            UUID eventId, Instant occurredOn,
            String productId, String variantId, String sku, String deletedBy
    ) {}

    public record VariantStockChanged(
            UUID eventId, Instant occurredOn,
            String productId, String variantId, String sku,
            int oldQuantity, int newQuantity, int delta, String updatedBy
    ) {}

    public record VariantPriceChanged(
            UUID eventId, Instant occurredOn,
            String productId, String variantId, String sku,
            BigDecimal oldPrice, BigDecimal newPrice, BigDecimal newSalePrice, String updatedBy
    ) {}

    public record VariantSnapshot(String variantId, String sku, BigDecimal price, int quantity) {}
}
