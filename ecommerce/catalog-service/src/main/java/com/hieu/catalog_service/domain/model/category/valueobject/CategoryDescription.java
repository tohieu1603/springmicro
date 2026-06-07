package com.hieu.catalog_service.domain.model.category.valueobject;

/** Optional category description. Null and blank normalise to the same value. */
public record CategoryDescription(String value) {

    public CategoryDescription {
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            } else if (value.length() > 500) {
                throw new IllegalArgumentException("Description cannot exceed 500 characters");
            }
        }
    }

    public static CategoryDescription of(String value) {
        return new CategoryDescription(value);
    }
}
