package com.hieu.order_service.domain.model.order.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Immutable monetary value (VND, 2dp). */
public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Money cannot be negative");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) { return value == null ? ZERO : new Money(value); }
    public static Money of(String value)     { return value == null ? ZERO : new Money(new BigDecimal(value)); }
    public static Money of(long value)       { return new Money(BigDecimal.valueOf(value)); }

    public Money add(Money other) { return new Money(amount.add(other.amount)); }
    public Money subtract(Money other) {
        var result = amount.subtract(other.amount);
        return new Money(result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result);
    }
    public Money multiply(int qty) {
        if (qty < 0) throw new IllegalArgumentException("multiply qty must be non-negative, got: " + qty);
        return new Money(amount.multiply(BigDecimal.valueOf(qty)));
    }

    public boolean isZero() { return amount.compareTo(BigDecimal.ZERO) == 0; }
    public boolean isWithin(Money other, BigDecimal tolerance) {
        return amount.subtract(other.amount).abs().compareTo(tolerance) <= 0;
    }
}
