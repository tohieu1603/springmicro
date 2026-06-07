package com.hieu.catalog_service.domain.model.category.valueobject;

/** Display name for a category. Trimmed; max 100 chars to fit the DB column. */
public record CategoryName(String value) {

    public CategoryName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        value = value.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("Category name cannot exceed 100 characters");
        }
    }

    public static CategoryName of(String value) {
        return new CategoryName(value);
    }
}
