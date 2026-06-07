package com.hieu.catalog_service.domain.model.product.valueobject;

/**
 * Lifecycle states for an individual variant.
 *
 * <p>{@link #OUT_OF_STOCK} is distinguished from {@link #INACTIVE}: the former is
 * auto-driven by stock (domain recovers to {@link #ACTIVE} when stock returns), while
 * {@link #INACTIVE} reflects a deliberate merchandising decision that stock changes
 * do NOT override.
 */
public enum VariantStatus {
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK;

    public static VariantStatus fromString(String status) {
        if (status == null) return ACTIVE;
        return switch (status.toUpperCase()) {
            case "INACTIVE" -> INACTIVE;
            case "OUT_OF_STOCK" -> OUT_OF_STOCK;
            default -> ACTIVE;
        };
    }

    public boolean canSell() {
        return this == ACTIVE;
    }
}
