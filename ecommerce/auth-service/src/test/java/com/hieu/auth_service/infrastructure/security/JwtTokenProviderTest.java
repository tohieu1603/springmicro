package com.hieu.auth_service.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.AccountStatus;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.services.TokenProviderPort.AccessClaims;
import com.hieu.auth_service.domain.services.TokenProviderPort.IssuedAccessToken;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Pure unit tests for the HMAC (HS256) {@link JwtTokenProvider}: issue -> parse round-trip,
 * claim packing (jti/userId/tokenVersion/roles/expiry), issuer enforcement, and rejection of
 * tampered / wrong-key / wrong-issuer / expired / malformed tokens. No Spring, no network.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "0123456789-abcdefghij-0123456789-abcdefghij"; // >= 32 chars
    private static final String ISSUER = "hieu.com";

    private JwtProperties props(String secret, long accessSeconds, String issuer) {
        return new JwtProperties(secret, accessSeconds, 7, issuer);
    }

    private JwtTokenProvider provider() {
        return new JwtTokenProvider(props(SECRET, 900L, ISSUER));
    }

    private User user() {
        UserId id = UserId.of("11111111-1111-1111-1111-111111111111");
        return User.reconstitute(
                id,
                Username.of("alice"),
                Email.of("alice@example.com"),
                Password.createEncoded("$2a$10$hashhashhashhashhashha"),
                PersonName.of("Alice", "Smith"),
                AccountStatus.createActive(),
                Set.of(),
                3,
                null,
                Instant.now(),
                Instant.now());
    }

    @Test
    void issueAccessToken_populatesMetadata() {
        JwtTokenProvider p = provider();

        IssuedAccessToken issued = p.issueAccessToken(user(), Set.of("ROLE_USER", "ROLE_ADMIN"));

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.tokenId()).isNotBlank();
        assertThat(issued.expiresInSeconds()).isEqualTo(900L);
        // expiresAt ~ now + 900s
        assertThat(issued.expiresAt()).isBetween(Instant.now().plusSeconds(890), Instant.now().plusSeconds(910));
    }

    @Test
    void issueThenParse_roundTripsAllClaims() {
        JwtTokenProvider p = provider();
        Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");

        IssuedAccessToken issued = p.issueAccessToken(user(), roles);
        AccessClaims claims = p.parseAccessToken(issued.token());

        assertThat(claims.tokenId()).isEqualTo(issued.tokenId());
        assertThat(claims.userId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(claims.username()).isEqualTo("alice");
        assertThat(claims.tokenVersion()).isEqualTo(3);
        assertThat(claims.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(claims.expiresAt()).isEqualTo(issued.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void parse_withEmptyRoles_returnsEmptySet() {
        JwtTokenProvider p = provider();
        IssuedAccessToken issued = p.issueAccessToken(user(), Set.of());

        AccessClaims claims = p.parseAccessToken(issued.token());

        assertThat(claims.roles()).isEmpty();
    }

    @Test
    void isSignatureValid_trueForOwnToken_falseForGarbage() {
        JwtTokenProvider p = provider();
        IssuedAccessToken issued = p.issueAccessToken(user(), Set.of("ROLE_USER"));

        assertThat(p.isSignatureValid(issued.token())).isTrue();
        assertThat(p.isSignatureValid("not-a-jwt")).isFalse();
    }

    @Test
    void parse_rejectsTokenSignedWithDifferentKey() {
        JwtTokenProvider p = provider();
        // Build a structurally valid token using a different HMAC key but the same issuer.
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "ZZZZZZZZZZ-different-secret-key-ZZZZZZZZZZ".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .id("jti-1")
                .subject("alice")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertThat(p.isSignatureValid(forged)).isFalse();
        assertThatThrownBy(() -> p.parseAccessToken(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void parse_rejectsTokenWithWrongIssuer_evenWhenSignedWithSameKey() {
        JwtTokenProvider p = provider();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String otherIssuerToken = Jwts.builder()
                .id("jti-2")
                .subject("alice")
                .issuer("payment-service")          // shares the key but not the issuer
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> p.parseAccessToken(otherIssuerToken)).isInstanceOf(JwtException.class);
        assertThat(p.isSignatureValid(otherIssuerToken)).isFalse();
    }

    @Test
    void parse_rejectsExpiredToken() {
        JwtTokenProvider p = provider();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .id("jti-3")
                .subject("alice")
                .issuer(ISSUER)
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> p.parseAccessToken(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void parse_rejectsMalformedToken() {
        JwtTokenProvider p = provider();
        assertThatThrownBy(() -> p.parseAccessToken("aaa.bbb.ccc")).isInstanceOf(JwtException.class);
    }

    @Test
    void jwtProperties_rejectsShortSecret() {
        assertThatThrownBy(() -> props("too-short", 900L, ISSUER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void jwtProperties_fillsDefaultsForNonPositiveTtlAndBlankIssuer() {
        JwtProperties filled = new JwtProperties(SECRET, 0L, 0, "  ");
        assertThat(filled.accessExpirationSeconds()).isEqualTo(900L);
        assertThat(filled.refreshExpirationDays()).isEqualTo(7);
        assertThat(filled.issuer()).isEqualTo("hieu.com");
    }
}
