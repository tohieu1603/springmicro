package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class ProductStatusChangedEvent extends DomainEvent {

    private final String productId;
    private final ProductStatus oldStatus;
    private final ProductStatus newStatus;
    private final String updatedBy;

    public ProductStatusChangedEvent(String productId, ProductStatus oldStatus,
                                      ProductStatus newStatus, String updatedBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.oldStatus = Objects.requireNonNull(oldStatus, "oldStatus");
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus");
        this.updatedBy = updatedBy;
    }

    @Override public String aggregateId() { return productId; }
}
