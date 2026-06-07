package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ReturnRequestId(String value) {

    public ReturnRequestId {
        Objects.requireNonNull(value, "ReturnRequestId value");
        UUID.fromString(value); // validate UUID format
    }

    public static ReturnRequestId of(String value) { return new ReturnRequestId(value); }

    public static ReturnRequestId generate() { return new ReturnRequestId(UUID.randomUUID().toString()); }
}
