package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record ProductName(String value) {

    public ProductName {
        Objects.requireNonNull(value, "ProductName value");
        if (value.isBlank()) throw new IllegalArgumentException("ProductName must not be blank");
        value = value.trim();
    }

    public static ProductName of(String value) { return new ProductName(value); }
}
