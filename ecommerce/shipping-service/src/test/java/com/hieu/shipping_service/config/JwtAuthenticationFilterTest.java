package com.hieu.shipping_service.config;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter}: token extraction (cookie precedence
 * over header, Bearer prefix handling), and the populate/skip/clear branches. Uses a REAL
 * {@link JwtTokenValidator} over a local HMAC key (no network, no Spring) with tokens
 * minted by JJWT; servlet request/response/chain are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "0123456789-0123456789-0123456789-test-secret";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(new JwtTokenValidator(KEY));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static String token(String userId, String username, List<String> roles,
                                List<String> permissions, Date expiry) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(new Date(System.currentTimeMillis() - 1000))
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }

    private static String validToken(List<String> roles, List<String> perms) {
        return token("u-1", "alice", roles, perms, new Date(System.currentTimeMillis() + 60_000));
    }

    @Test
    @DisplayName("valid Bearer token populates the SecurityContext with principal + authorities")
    void validBearer_populatesContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + validToken(List.of("ROLE_ADMIN"), List.of("shipment:read")));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        var user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.userId()).isEqualTo("u-1");
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.roles()).containsExactly("ROLE_ADMIN");
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "shipment:read");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("token from ACCESS_TOKEN cookie is accepted (cookie precedence)")
    void cookieToken_accepted() throws Exception {
        var t = validToken(List.of("ROLE_USER"), List.of());
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", t)});

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).username()).isEqualTo("alice");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("no token leaves the context empty and still proceeds")
    void noToken_skips() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-Bearer Authorization header is ignored")
    void nonBearerHeader_skips() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token is rejected: context stays empty, chain still runs")
    void expiredToken_rejected() throws Exception {
        var expired = token("u-1", "alice", List.of("ROLE_USER"), List.of(),
                new Date(System.currentTimeMillis() - 1000));
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expired);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("tampered/invalid token clears the context and proceeds")
    void invalidToken_clearsContext() throws Exception {
        // signed with a DIFFERENT key -> signature verification fails -> isExpired() returns true (treats unparseable as expired)
        var otherKey = Keys.hmacShaKeyFor("another-secret-another-secret-32!!".getBytes(StandardCharsets.UTF_8));
        var foreign = Jwts.builder().subject("mallory")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey).compact();
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + foreign);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("missing roles/permissions claims default to empty authorities")
    void missingClaims_emptyAuthorities() throws Exception {
        var t = Jwts.builder().subject("bob").claim("userId", "u-2")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(KEY).compact();
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + t);

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
        var user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.roles()).isEmpty();
        assertThat(user.permissions()).isEmpty();
    }

    @Test
    @DisplayName("non-string roles claim values are coerced to strings")
    void numericRoleClaims_coercedToString() throws Exception {
        var t = Jwts.builder().subject("carol").claim("userId", "u-3")
                .claim("roles", List.of("ROLE_USER"))
                .claim("permissions", Map.of()) // not a List -> extractList yields empty
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(KEY).compact();
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + t);

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        var user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.roles()).containsExactly("ROLE_USER");
        assertThat(user.permissions()).isEmpty();
    }

    @Test
    @DisplayName("blank Bearer value is treated as no token")
    void blankBearer_skips() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
