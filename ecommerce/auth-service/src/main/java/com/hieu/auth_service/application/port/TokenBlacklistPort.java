package com.hieu.auth_service.application.port;

import java.time.Instant;

/**
 * Outbound port for access-token revocation lookup.
 *
 * <p>Used by auth-side use cases (logout, password change, admin revoke) and by the
 * request-path JWT filter. Infrastructure supplies a hybrid Redis+DB adapter so the
 * fast path (Redis) survives without losing durability (DB fallback).
 */
public interface TokenBlacklistPort {

    /**
     * Revokes an access token by its {@code jti}.
     *
     * @param tokenId   JWT jti claim
     * @param userId    owner of the token
     * @param expiresAt original expiry instant — used to size TTL and to purge stale rows
     * @param reason    LOGOUT | ADMIN_REVOKE | SECURITY_BREACH | PASSWORD_CHANGED
     */
    void revoke(String tokenId, String userId, Instant expiresAt, String reason);

    /**
     * @param tokenId JWT jti claim
     * @return {@code true} if the token has been revoked
     */
    boolean isRevoked(String tokenId);
}
