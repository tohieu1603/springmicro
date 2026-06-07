package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.UUID;

/** User UUID string wrapper — id is sourced from auth-service JWT (always UUID v4). */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value");
        if (value.isBlank()) throw new IllegalArgumentException("UserId must not be blank");
        value = value.trim();
        UUID.fromString(value);
    }

    public static UserId of(String value) { return new UserId(value); }
}
