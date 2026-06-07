package com.hieu.auth_service.domain.services;

/**
 * Outbound port for Google ID token verification.
 *
 * <p>Infrastructure supplies the adapter (Google's official google-api-client
 * library). Keeping the port in {@code domain.services} lets the Google login
 * handler stay framework-free and the verification mechanism stay swappable
 * (e.g. switch to JWKS-based verification, mock for tests, …).
 *
 * <p>The trimmed {@link GoogleClaims} record captures only what we actually
 * consume; raw token claims never cross the domain boundary.
 */
public interface GoogleIdTokenVerifierPort {

    /**
     * Validates the signature, expiry and audience of a Google-issued ID token
     * (the {@code credential} returned by Google Identity Services on the FE).
     *
     * @param rawToken the ID token string from {@code POST /api/auth/google}
     * @return the trimmed claims when verification succeeds
     * @throws com.hieu.auth_service.domain.models.user.exceptions.GoogleTokenInvalidException
     *         when signature / audience / expiry checks fail
     */ 
    GoogleClaims verify(String rawToken);

    /**
     * Subset of Google's standard ID-token claims we rely on.
     *
     * @param sub             stable, opaque user identifier (preferred for linking)
     * @param email           primary email address
     * @param emailVerified   whether Google has verified the email
     * @param name            full name (may be empty for some Workspace setups)
     * @param givenName       first name
     * @param familyName      last name
     * @param picture         avatar URL (may be null)
     */
    record GoogleClaims(
            String sub,
            String email,
            boolean emailVerified,
            String name,
            String givenName,
            String familyName,
            String picture
    ) {}
}
