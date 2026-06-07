package com.hieu.order_service.domain.model.order.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record RefundAmount(BigDecimal amount) {

    public static final RefundAmount ZERO = new RefundAmount(BigDecimal.ZERO);

    public RefundAmount {
        Objects.requireNonNull(amount, "RefundAmount");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("RefundAmount cannot be negative");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Null-tolerant — null collapses to ZERO, consistent with {@link Money#of(BigDecimal)}. */
    public static RefundAmount of(BigDecimal value) { return value == null ? ZERO : new RefundAmount(value); }
    public static RefundAmount of(String value)     { return value == null ? ZERO : new RefundAmount(new BigDecimal(value)); }

    /** Returns null for null input — use to preserve "not yet refunded" semantic in DB mapping. */
    public static RefundAmount ofNullable(BigDecimal value) { return value == null ? null : new RefundAmount(value); }
}
