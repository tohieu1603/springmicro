package com.hieu.auth_service.domain.models.refreshtoken.vo;

import java.util.UUID;

/**
 * Value Object representing Token value (The actual token string)
 */
public record TokenValue(String value) {

    public TokenValue {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TokenValue cannot be null or empty");
        }
        value = value.trim();
    }

    public static TokenValue of(String value) {
        return new TokenValue(value);
    }

    public static TokenValue generate() {
        return new TokenValue(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return "TokenValue{" + value.substring(0, Math.min(8, value.length())) + "...}";
    }
}