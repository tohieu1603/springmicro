package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/** Human-readable order reference: {@code ORD-yyyyMMdd-NNNNNN}. */
public record OrderNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^ORD-\\d{8}-\\d{6}$");

    public OrderNumber {
        Objects.requireNonNull(value, "OrderNumber value");
        if (value.isBlank()) throw new IllegalArgumentException("OrderNumber must not be blank");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("OrderNumber must match ORD-yyyyMMdd-NNNNNN, got: " + value);
        }
    }

    public static OrderNumber of(String value) { return new OrderNumber(value); }
}
