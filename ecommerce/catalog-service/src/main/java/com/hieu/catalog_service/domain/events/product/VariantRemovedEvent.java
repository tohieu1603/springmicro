package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class VariantRemovedEvent extends DomainEvent {

    private final String productId;
    private final String variantId;
    private final String sku;
    private final String deletedBy;

    public VariantRemovedEvent(String productId, String variantId, String sku, String deletedBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.variantId = Objects.requireNonNull(variantId, "variantId");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.deletedBy = deletedBy;
    }

    @Override public String aggregateId() { return productId; }
}
