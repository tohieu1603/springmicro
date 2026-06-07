package com.hieu.order_service.domain.model.order.valueobject;

/** Positive item quantity, bounded to a sane retail upper limit. */
public record Quantity(int value) {

    /** Practical upper bound — bulk orders beyond this are caught at API boundary as suspicious. */
    public static final int MAX = 999_999;

    public Quantity {
        if (value <= 0) throw new IllegalArgumentException("Quantity must be positive, got: " + value);
        if (value > MAX) throw new IllegalArgumentException("Quantity exceeds max " + MAX + ", got: " + value);
    }

    public static Quantity of(int value) { return new Quantity(value); }
}
