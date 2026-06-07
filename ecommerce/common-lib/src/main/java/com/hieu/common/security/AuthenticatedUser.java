package com.hieu.common.security;

import java.util.List;

/**
 * Authenticated user context extracted from a JWT and set as the Spring Security principal
 * by downstream services (non-auth-service). Produced by {@link JwtTokenValidator} inside a
 * {@link com.hieu.common.security.JwtAuthenticationFilter}.
 *
 * <p>{@code userId} is a UUID {@link String} to match the auth-service aggregate id type.
 * Roles always come from JWT claims (always available). Permissions are optional — they
 * can be resolved from a cache or omitted in degraded mode.
 *
 * @param userId      stable user UUID
 * @param username    unique username
 * @param roles       role names (e.g. {@code ROLE_USER})
 * @param permissions optional permission names; empty list means degraded mode
 */
public record AuthenticatedUser(
        String userId,
        String username,
        List<String> roles,
        List<String> permissions
) {
    public AuthenticatedUser {
        roles       = roles       == null ? List.of() : List.copyOf(roles);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    /** Convenience factory when the caller has no permissions resolved. */
    public static AuthenticatedUser of(String userId, String username, List<String> roles) {
        return new AuthenticatedUser(userId, username, roles, List.of());
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... candidates) {
        for (String r : candidates) if (roles.contains(r)) return true;
        return false;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasAnyPermission(String... candidates) {
        for (String p : candidates) if (permissions.contains(p)) return true;
        return false;
    }
}
