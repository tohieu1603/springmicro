package com.hieu.catalog_service.domain.events.product;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class ProductUpdatedEvent extends DomainEvent {

    private final String productId;
    private final String name;
    private final String slug;
    private final String updatedBy;

    public ProductUpdatedEvent(String productId, String name, String slug, String updatedBy) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.name = Objects.requireNonNull(name, "name");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.updatedBy = updatedBy;
    }

    @Override public String aggregateId() { return productId; }
}
