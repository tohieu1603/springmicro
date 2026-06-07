package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record RecipientName(String value) {

    public RecipientName {
        Objects.requireNonNull(value, "RecipientName");
        if (value.isBlank()) throw new IllegalArgumentException("RecipientName must not be blank");
        value = value.trim();
    }

    public static RecipientName of(String value) { return new RecipientName(value); }
}
