package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record ReturnReason(String value) {

    public ReturnReason {
        Objects.requireNonNull(value, "ReturnReason");
        if (value.isBlank()) throw new IllegalArgumentException("ReturnReason must not be blank");
    }

    public static ReturnReason of(String value) { return new ReturnReason(value); }
}
