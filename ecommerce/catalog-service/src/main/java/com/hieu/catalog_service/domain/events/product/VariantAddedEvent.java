package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
public final class VariantAddedEvent extends DomainEvent {

    private final String productId;
    private final String variantId;
    private final String sku;
    private final BigDecimal price;
    private final int quantity;
    private final String createdBy;

    public VariantAddedEvent(String productId, String variantId, String sku, BigDecimal price,
                              int quantity, String createdBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.variantId = Objects.requireNonNull(variantId, "variantId");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.price = Objects.requireNonNull(price, "price");
        this.quantity = quantity;
        this.createdBy = createdBy;
    }

    @Override public String aggregateId() { return productId; }
}
