package com.hieu.auth_service.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Hashes refresh-token secrets before they are persisted.
 *
 * <p>Refresh tokens are high-entropy random UUIDs (122 bits) handed to the client; only a
 * one-way digest is stored server-side, so a database dump cannot be replayed to hijack live
 * sessions. Lookups hash the presented value and compare against the stored digest.
 *
 * <p>A plain (unsalted) SHA-256 is deliberate: the input is already unguessable, so per-row
 * salting would buy nothing while making by-value lookups impossible. This is the standard
 * treatment for opaque API-key / session-token storage — not for human passwords (those use
 * BCrypt via {@code BCryptPasswordEncoderAdapter}).
 */
@Component
public class RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Returns the lower-case hex SHA-256 digest of the raw token.
     *
     * @param rawToken the opaque token value handed to the client (never {@code null}/blank)
     * @return 64-char hex digest suitable for storage + indexed lookup
     */
    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken must not be null or blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS on every conformant JVM — unreachable in practice.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
