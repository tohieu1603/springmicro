package com.hieu.auth_service.domain.models.permission.vo;

/**
 * Value Object representing Permission Name
 * Format: RESOURCE_ACTION (e.g., USER_READ, PRODUCT_UPDATE,...)
 */
public record PermissionName(String resource, String action, String value) {

    public PermissionName(String resource, String action) {
        this(
                resource != null ? resource.trim().toUpperCase() : null,
                action != null ? action.trim().toUpperCase() : null,
                (resource != null && action != null)
                        ? (resource.trim().toUpperCase() + "_" + action.trim().toUpperCase())
                        : null
        );

        if (this.resource == null || this.resource.isEmpty()) {
            throw new IllegalArgumentException("Permission resource cannot be empty");
        }
        if (this.action == null || this.action.isEmpty()) {
            throw new IllegalArgumentException("Permission action cannot be empty");
        }
    }

    public static PermissionName of(String resource, String action) {
        return new PermissionName(resource, action);
    }

    /**
     * Create from formatted string (e.g., "USER_READ")
     */
    public static PermissionName fromString(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name cannot be empty");
        }

        String[] parts = permission.trim().toUpperCase().split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid permission format. Expected: RESOURCE_ACTION, got: " + permission
            );
        }
        return new PermissionName(parts[0], parts[1]);
    }

    /**
     * Check if this permission applies to a specific resource
     */
    public boolean isForResource(String resource) {
        return this.resource.equalsIgnoreCase(resource);
    }

    /**
     * Check if this permission applies specific action
     */
    public boolean allowsAction(String action) {
        return this.action.equalsIgnoreCase(action);
    }

    @Override
    public String toString() {
        return value;
    }
}