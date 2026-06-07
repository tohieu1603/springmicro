package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "IdempotencyKey value");
        if (value.isBlank()) throw new IllegalArgumentException("IdempotencyKey must not be blank");
        value = value.trim();
    }

    /** Strict factory — throws on null/blank. */
    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    /** Lenient factory — returns null for null/blank input (for optional idempotency tracking). */
    public static IdempotencyKey ofNullable(String value) {
        return (value == null || value.isBlank()) ? null : new IdempotencyKey(value);
    }
}
