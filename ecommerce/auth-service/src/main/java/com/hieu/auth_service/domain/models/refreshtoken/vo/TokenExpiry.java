package com.hieu.auth_service.domain.models.refreshtoken.vo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Value Object representing Token Expiry Date
 * Encapsulates expiry validation logic
 */
public record TokenExpiry(Instant expiryDate) {

    public TokenExpiry {
        if (expiryDate == null) {
            throw new IllegalArgumentException("ExpiryDate must not be null");
        }
    }

    public static TokenExpiry of(Instant expiryDate) {
        return new TokenExpiry(expiryDate);
    }

    /**
     * Create expiry date from now + days
     */
    public static TokenExpiry fromDaysFromNow(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        return new TokenExpiry(Instant.now().plus(days, ChronoUnit.DAYS));
    }

    /**
     * Create expiry date from now + hours
     */
    public static TokenExpiry fromHoursFromNow(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        return new TokenExpiry(Instant.now().plus(hours, ChronoUnit.HOURS));
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Check if token is valid (not expired)
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Get remaining time in seconds
     */
    public long getRemainingSeconds() {
        if (isExpired()) {
            return 0;
        }
        return Instant.now().until(expiryDate, ChronoUnit.SECONDS);
    }

    /**
     * Check if token will expire within given seconds
     */
    public boolean willExpireWithin(long seconds) {
        return getRemainingSeconds() <= seconds;
    }

    @Override
    public String toString() {
        return "TokenExpiry{" +
                "expiryDate=" + expiryDate +
                ", isExpired=" + isExpired() +
                ", remaining seconds=" + getRemainingSeconds() +
                "}";
    }
}