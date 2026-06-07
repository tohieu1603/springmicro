package com.hieu.catalog_service.domain.model.category.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed category identifier backed by a UUID string.
 *
 * <p>Aggregate factory creates {@code Category} with {@code id=null} (app assigns via
 * {@code @PrePersist}). The VO itself rejects null — null id fields bypass this VO,
 * but any code that explicitly wraps an id must provide a real value.
 */
public record CategoryId(String value) {

    public CategoryId {
        Objects.requireNonNull(value, "CategoryId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CategoryId cannot be blank");
        }
    }

    public static CategoryId of(String value) {
        return new CategoryId(value);
    }

    public static CategoryId generate() {
        return new CategoryId(UUID.randomUUID().toString());
    }
}
