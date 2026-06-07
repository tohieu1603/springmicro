package com.hieu.catalog_service.domain.model.product.valueobject;

/**
 * Non-negative stock count. Catalog owns a snapshot — authoritative stock lives in the
 * inventory-service; this VO tolerates that drift but forbids going negative locally.
 */
public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Quantity add(int delta) {
        return new Quantity(Math.addExact(this.value, delta));
    }

    public Quantity subtract(int delta) {
        int next = Math.subtractExact(this.value, delta);
        if (next < 0) {
            throw new IllegalArgumentException("Insufficient quantity: have " + value + ", requested " + delta);
        }
        return new Quantity(next);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isLowStock(int threshold) {
        return value > 0 && value <= threshold;
    }
}
