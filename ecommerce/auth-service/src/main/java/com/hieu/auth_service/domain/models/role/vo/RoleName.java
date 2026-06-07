package com.hieu.auth_service.domain.models.role.vo;

/**
 * Value Object representing Role Name
 * Ensures role name follows naming conventions (e.g., ROLE_ADMIN, ROLE_USER)
 */
public record RoleName(String value) {

    private static final String ROLE_PREFIX = "ROLE_";

    // Compact constructor thực hiện logic chuẩn hóa chuỗi
    public RoleName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }

        String trimmedValue = value.trim().toUpperCase();
        if (!trimmedValue.startsWith(ROLE_PREFIX)) {
            trimmedValue = ROLE_PREFIX + trimmedValue;
        }

        value = trimmedValue;
    }

    public static RoleName of(String value) {
        return new RoleName(value);
    }

    /**
     * Common Role names
     */
    public static RoleName admin() {
        return new RoleName(ROLE_PREFIX + "ADMIN");
    }

    public static RoleName user() {
        return new RoleName(ROLE_PREFIX + "USER");
    }

    /**
     * Get role name without ROLE_ prefix
     */
    public String getSimpleName() {
        return value.substring(ROLE_PREFIX.length());
    }

    @Override
    public String toString() {
        return value;
    }
}