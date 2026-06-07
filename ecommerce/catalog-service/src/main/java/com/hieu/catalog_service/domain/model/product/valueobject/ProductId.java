package com.hieu.catalog_service.domain.model.product.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed product identifier backed by a UUID string on the persistence side.
 *
 * <p>Using a dedicated VO instead of a raw {@code String} prevents accidental mixing of
 * {@link ProductId} / {@link VariantId} / {@link com.hieu.catalog_service.domain.model.category.valueobject.CategoryId}
 * at method signatures — the compiler catches it instead of a runtime bug.
 */
public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProductId cannot be blank");
        }
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }
}
