package com.hieu.auth_service.domain.models.permission.vo;

import java.util.UUID;

public record PermissionId(String value) {

    public PermissionId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("PermissionId cannot be null or empty");
        }
        value = value.trim();
        UUID.fromString(value);
    }

    @Override
    public String toString() { return value; }
    public static PermissionId generate() {
        return new PermissionId(UUID.randomUUID().toString());
    }
    public static PermissionId of(String value) {
        return new PermissionId(value);
    }
}