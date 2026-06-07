package com.hieu.auth_service.domain.services;

import com.hieu.auth_service.domain.models.user.User;

import java.time.Instant;
import java.util.Set;

/**
 * Outbound port for access-token issuance and inspection.
 *
 * <p>Implemented by infrastructure using JWT (HS256/EdDSA/…) or any other token
 * scheme. Domain + application stay agnostic about the underlying crypto.
 */
public interface TokenProviderPort {

    /**
     * A freshly issued access token plus its validity metadata.
     *
     * @param token             opaque access token string
     * @param tokenId           unique token identifier ({@code jti} for JWT)
     * @param expiresAt         absolute expiry instant
     * @param expiresInSeconds  remaining lifetime in seconds at issue time
     */
    record IssuedAccessToken(String token, String tokenId, Instant expiresAt, long expiresInSeconds) {}

    /**
     * Parsed claims extracted from a valid access token.
     *
     * @param tokenId      token id ({@code jti})
     * @param userId       subject / user identifier
     * @param username     username claim, may be empty
     * @param tokenVersion user's tokenVersion at the time the token was issued
     * @param roles        role names granted to the user at issue time
     * @param expiresAt    absolute expiry instant
     */
    record AccessClaims(String tokenId, String userId, String username,
                         int tokenVersion, Set<String> roles, Instant expiresAt) {}

    /**
     * Issues a new access token for the given user.
     *
     * @param user  non-null user aggregate
     * @param roles role names to embed; typically collected by the application layer
     * @return issued token + metadata
     */
    IssuedAccessToken issueAccessToken(User user, Set<String> roles);

    /**
     * Parses + validates the signature and expiry of an access token.
     *
     * @param token raw access token string
     * @return parsed claims
     * @throws RuntimeException when the token is invalid, malformed, or expired
     *                          (implementations SHOULD throw library-specific exceptions
     *                          so the global handler can map them uniformly)
     */
    AccessClaims parseAccessToken(String token);
}
