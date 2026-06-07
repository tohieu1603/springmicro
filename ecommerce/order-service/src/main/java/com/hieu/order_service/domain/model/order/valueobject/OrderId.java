package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.UUID;

/** Surrogate PK wrapper — UUID string, app-assigned. */
public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value");
        UUID.fromString(value); // validate UUID format
    }

    public static OrderId of(String value) { return new OrderId(value); }

    public static OrderId generate() { return new OrderId(UUID.randomUUID().toString()); }
}
