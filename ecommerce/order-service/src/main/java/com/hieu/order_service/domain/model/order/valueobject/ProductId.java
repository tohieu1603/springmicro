package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId value");
        UUID.fromString(value); // validate UUID format
    }

    public static ProductId of(String value) { return new ProductId(value); }

    public static ProductId generate() { return new ProductId(UUID.randomUUID().toString()); }
}
