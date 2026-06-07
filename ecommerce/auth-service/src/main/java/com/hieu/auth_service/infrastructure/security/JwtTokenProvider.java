package com.hieu.auth_service.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.services.TokenProviderPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

/**
 * JWT (HS256) implementation of {@link TokenProviderPort}.
 *
 * <p>Access tokens are self-contained JWTs with these custom claims:
 * <ul>
 *   <li>{@code jti}          — unique token id; blacklist key on revocation</li>
 *   <li>{@code userId}       — owning user UUID as String (previous code incorrectly
 *       typed this as {@code Long} and silently returned {@code null})</li>
 *   <li>{@code tokenVersion} — user's current tokenVersion; bumped on password change
 *       to invalidate every outstanding access token atomically</li>
 *   <li>{@code roles}        — role names granted at issue time</li>
 * </ul>
 *
 * <p>Refresh tokens are NOT JWTs — they are server-tracked opaque values managed by
 * the {@code RefreshToken} aggregate; this class only deals with access tokens.
 */
@Component
public class JwtTokenProvider implements TokenProviderPort {

    /** Force HS256 — jjwt otherwise picks HS384/HS512 based on secret length. */
    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a fresh access token for the given user.
     *
     * @param user  non-null domain user
     * @param roles role names to embed; callers normally collect them from RoleRepository
     * @return issued access token + metadata
     */
    @Override
    public IssuedAccessToken issueAccessToken(User user, Set<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessExpirationSeconds());
        String tokenId = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", tokenId);
        claims.put("userId", user.getId().value());
        claims.put("email", user.getEmail().value());
        claims.put("tokenVersion", user.getTokenVersion());
        claims.put("roles", roles);

        String token = Jwts.builder()
                .id(tokenId)
                .claims(claims)
                .subject(user.getUsername().value())
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, ALGORITHM)
                .compact();

        return new IssuedAccessToken(token, tokenId, exp, props.accessExpirationSeconds());
    }

    /**
     * Parses + signature-verifies an access token and returns its claims.
     *
     * @throws io.jsonwebtoken.JwtException on signature mismatch, expiry, or malformed input
     */
    @Override
    public AccessClaims parseAccessToken(String token) {
        Claims c = parseClaims(token);
        @SuppressWarnings("unchecked")
        Set<String> roles = Set.copyOf((Collection<String>) c.getOrDefault("roles", Set.of()));
        int tv = ((Number) c.getOrDefault("tokenVersion", 0)).intValue();
        String userId = c.get("userId", String.class);
        return new AccessClaims(
                c.getId(),
                userId != null ? userId : c.getSubject(),
                c.getSubject(),
                tv,
                roles,
                c.getExpiration().toInstant());
    }

    /** Cheap signature-only validation that never throws — useful inside filters. */
    public boolean isSignatureValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        // requireIssuer guards against tokens minted by another service that shares the
        // same secret (e.g. payment-service signs internal-only tokens with the same
        // HS256 key). Without this enforcement, those tokens would be accepted as
        // user-auth tokens here.
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
