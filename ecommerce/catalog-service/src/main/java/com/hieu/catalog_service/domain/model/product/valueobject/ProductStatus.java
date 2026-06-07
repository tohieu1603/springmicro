package com.hieu.catalog_service.domain.model.product.valueobject;

/**
 * Lifecycle states for a product SPU.
 *
 * <ul>
 *   <li>{@link #DRAFT} — created, not yet published.</li>
 *   <li>{@link #ACTIVE} — listed and sellable (if at least one variant is available).</li>
 *   <li>{@link #INACTIVE} — hidden from catalog but kept for history/reporting.</li>
 *   <li>{@link #DELETED} — soft-deleted; repositories filter it out of default queries.</li>
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    DELETED;

    public static ProductStatus fromString(String status) {
        if (status == null) return DRAFT;
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> ACTIVE;
            case "INACTIVE" -> INACTIVE;
            case "DELETED" -> DELETED;
            default -> DRAFT;
        };
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canSell() {
        return this == ACTIVE;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }
}
