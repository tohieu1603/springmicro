package com.hieu.auth_service.domain.models.role.vo;

import java.util.UUID;

/**
 * Value Object representing Role Identity
 */
public record RoleId(String value) {

    public RoleId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("RoleId cannot be null or empty");
        }
        value = value.trim();
        UUID.fromString(value);
    }
    public static RoleId generate() {
        return new RoleId(UUID.randomUUID().toString());
    }
    public static RoleId of(String value) {
        return new RoleId(value);
    }

    @Override
    public String toString() { return value; }
}