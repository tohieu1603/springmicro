package com.hieu.catalog_service.domain.model.attribute.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for an {@code attrs} row (an attribute definition).
 *
 * <p>UUID string — aggregate factory creates {@code Attr} with id=null (app assigns via
 * {@code @PrePersist}). VO itself rejects null for consistency with sibling Id VOs.
 */
public record AttrId(String value) {

    public AttrId {
        Objects.requireNonNull(value, "AttrId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("AttrId cannot be blank");
        }
    }

    public static AttrId of(String value) {
        return new AttrId(value);
    }

    public static AttrId generate() {
        return new AttrId(UUID.randomUUID().toString());
    }
}
