package com.hieu.auth_service.interfaces.rest.dto;

import com.hieu.auth_service.infrastructure.security.AuthUserDetails;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lean "who am I" payload derived from the authenticated {@link AuthUserDetails}.
 *
 * <p>Exposes only what the client needs — no password hash, no account-status flags
 * (those live on {@code GET /api/users/me}). This endpoint is meant for cheap
 * front-end calls that just need "am I logged in / which role do I have".
 *
 * @param userId         stable user UUID as string
 * @param username       unique username
 * @param email          primary email
 * @param tokenVersion   current token version (monotonic)
 * @param authorities    flat set of granted authority names (roles + permissions)
 */
public record AuthMeResponse(
        String userId,
        String username,
        String email,
        int tokenVersion,
        Set<String> authorities
) {
    /** Projects the authenticated principal onto a safe response body. */
    public static AuthMeResponse from(AuthUserDetails principal) {
        Set<String> granted = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
        return new AuthMeResponse(
                principal.userId(),
                principal.getUsername(),
                principal.email(),
                principal.tokenVersion(),
                granted);
    }
}
