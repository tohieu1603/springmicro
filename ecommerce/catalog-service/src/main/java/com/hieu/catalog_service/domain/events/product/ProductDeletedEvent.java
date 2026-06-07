package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class ProductDeletedEvent extends DomainEvent {

    private final String productId;
    private final List<String> variantIds;
    private final String deletedBy;

    public ProductDeletedEvent(String productId, List<String> variantIds, String deletedBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.variantIds = List.copyOf(Objects.requireNonNullElse(variantIds, List.of()));
        this.deletedBy = deletedBy;
    }

    @Override public String aggregateId() { return productId; }
}
