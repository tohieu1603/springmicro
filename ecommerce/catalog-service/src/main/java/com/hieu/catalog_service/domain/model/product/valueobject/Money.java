package com.hieu.catalog_service.domain.model.product.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Non-negative monetary amount, scaled to the currency's minor units.
 *
 * <p>Scale is pinned at 2 (cents) and any incoming value is rounded
 * {@link RoundingMode#HALF_UP} so arithmetic stays stable across write/read round-trips.
 * Currency is implicit — the service assumes a single store currency; introduce a
 * {@code Currency} VO before changing that.
 */
public record Money(BigDecimal amount) {

    private static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "Money amount cannot be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + amount);
        }
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Strict factory — throws if amount is null. Use for required fields. */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "Money amount required");
        return new Money(amount);
    }

    /** Lenient factory — returns null for null input. Use for nullable fields like salePrice. */
    public static Money ofNullable(BigDecimal amount) {
        return amount == null ? null : new Money(amount);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other");
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other");
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("Money subtract would go negative: " + this + " - " + other);
        }
        return new Money(result);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }
}
