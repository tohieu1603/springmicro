package com.hieu.catalog_service.domain.model.attribute.valueobject;

/**
 * Shape of an attribute definition.
 *
 * <ul>
 *   <li>{@link #SELECT} — value must come from the predefined {@code attr_vals} list.</li>
 *   <li>{@link #TEXT}   — variant may store a free-text value.</li>
 *   <li>{@link #NUMBER} — free-text but numeric-only (weight, length, etc).</li>
 * </ul>
 */
public enum AttrType {
    SELECT,
    TEXT,
    NUMBER;

    public static AttrType fromString(String type) {
        if (type == null) return SELECT;
        return switch (type.toUpperCase()) {
            case "TEXT" -> TEXT;
            case "NUMBER" -> NUMBER;
            default -> SELECT;
        };
    }

    /** {@code true} if the variant may provide any text value instead of picking an {@code AttrVal}. */
    public boolean allowsFreeText() {
        return this == TEXT || this == NUMBER;
    }
}
