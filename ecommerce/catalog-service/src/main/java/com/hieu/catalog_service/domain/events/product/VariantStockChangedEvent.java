package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

/**
 * Local stock snapshot changed. Canonical stock lives in inventory-service; this event
 * only reflects the catalog-side cache and drives search/analytics cache invalidation.
 */
@Getter
public final class VariantStockChangedEvent extends DomainEvent {

    private final String productId;
    private final String variantId;
    private final String sku;
    private final int oldQuantity;
    private final int newQuantity;
    private final String updatedBy;

    public VariantStockChangedEvent(String productId, String variantId, String sku,
                                     int oldQuantity, int newQuantity, String updatedBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.variantId = Objects.requireNonNull(variantId, "variantId");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.oldQuantity = oldQuantity;
        this.newQuantity = newQuantity;
        this.updatedBy = updatedBy;
    }

    @Override public String aggregateId() { return productId; }

    public int getDelta() { return newQuantity - oldQuantity; }
}
