package com.hieu.auth_service.domain.models.refreshtoken.vo;

import java.util.UUID;

public record TokenId(String value) {

    public TokenId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TokenId cannot be null or empty");
        }
        value = value.trim();
        UUID.fromString(value);
    }

    @Override
    public String toString() { return value; }
    public static TokenId generate() {
        return new TokenId(UUID.randomUUID().toString());
    }
    public static TokenId of(String value) {
        return new TokenId(value);
    }
}