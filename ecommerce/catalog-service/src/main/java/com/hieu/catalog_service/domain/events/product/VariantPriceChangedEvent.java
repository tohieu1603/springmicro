package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
public final class VariantPriceChangedEvent extends DomainEvent {

    private final String productId;
    private final String variantId;
    private final String sku;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
    private final BigDecimal newSalePrice;
    private final String updatedBy;

    public VariantPriceChangedEvent(String productId, String variantId, String sku,
                                     BigDecimal oldPrice, BigDecimal newPrice,
                                     BigDecimal newSalePrice, String updatedBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.variantId = Objects.requireNonNull(variantId, "variantId");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.oldPrice = oldPrice;
        this.newPrice = Objects.requireNonNull(newPrice, "newPrice");
        this.newSalePrice = newSalePrice;
        this.updatedBy = updatedBy;
    }

    @Override public String aggregateId() { return productId; }
}
