package com.hieu.catalog_service.domain.model.product.valueobject;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed variant (SKU row) identifier. See {@link ProductId} for rationale. */
public record VariantId(String value) {

    public VariantId {
        Objects.requireNonNull(value, "VariantId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("VariantId cannot be blank");
        }
    }

    public static VariantId of(String value) {
        return new VariantId(value);
    }

    public static VariantId generate() {
        return new VariantId(UUID.randomUUID().toString());
    }
}
