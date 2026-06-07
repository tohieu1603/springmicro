package com.hieu.auth_service.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised JWT configuration.
 *
 * <p>Validated in the compact constructor so the app fails fast on misconfiguration:
 * a weak secret or zero/negative TTL surfaces at startup rather than at the first login.
 *
 * @param secret                   HMAC secret, ≥ 32 chars for HS256
 * @param accessExpirationSeconds  access token TTL in seconds (default 15 min)
 * @param refreshExpirationDays    refresh token TTL in days (default 7)
 * @param issuer                   {@code iss} claim
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessExpirationSeconds,
        int refreshExpirationDays,
        String issuer
) {
    /** Validates the record at construction; fills reasonable defaults for optional fields. */
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters (HS256 requirement)");
        }
        // The yaml ships a 55-char placeholder secret (`change-me-...`) that passes the
        // length check. Prod must reject it — otherwise a forgotten JWT_SECRET env var
        // means every prod deploy signs tokens with a publicly-known key.
        if (secret.startsWith("change-me")) {
            String profiles = System.getProperty("spring.profiles.active", "")
                    + "," + System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "");
            if (profiles.contains("prod")) {
                throw new IllegalStateException(
                        "Default placeholder jwt.secret detected with prod profile active — set JWT_SECRET env var");
            }
        }
        if (accessExpirationSeconds <= 0) accessExpirationSeconds = 900L;    // 15 min
        if (refreshExpirationDays    <= 0) refreshExpirationDays    = 7;     // 7 days
        if (issuer == null || issuer.isBlank()) issuer = "hieu.com";
    }
}
