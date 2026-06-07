package com.hieu.cart_service.infrastructure.security;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter}: token extraction (header vs cookie),
 * expiry/invalid-signature handling, claim -> {@link AuthenticatedUser} mapping and
 * authority derivation. {@link JwtTokenValidator}, {@link Claims} and the servlet
 * contracts are all mocked; the filter chain is always asserted to proceed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenValidator validator;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(validator);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("valid Bearer token populates the SecurityContext with a fully-mapped principal")
    void validBearerToken_authenticates() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("u1");
        when(claims.getSubject()).thenReturn("alice");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
        when(claims.get("permissions")).thenReturn(List.of("cart:write"));

        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(validator.isExpired("good.token")).thenReturn(false);
        when(validator.parseClaims("good.token")).thenReturn(claims);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = currentAuth();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("u1");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.roles()).containsExactly("ROLE_USER");
        assertThat(principal.permissions()).containsExactly("cart:write");
        assertThat(auth.getCredentials()).isEqualTo("good.token");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "cart:write");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("token from the ACCESS_TOKEN cookie is used in preference to the header")
    void cookieToken_authenticates() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("u2");
        when(claims.getSubject()).thenReturn("bob");
        when(claims.get("roles")).thenReturn(null);
        when(claims.get("permissions")).thenReturn(null);

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie.token")});
        when(validator.isExpired("cookie.token")).thenReturn(false);
        when(validator.parseClaims("cookie.token")).thenReturn(claims);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = currentAuth();
        assertThat(auth).isNotNull();
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("u2");
        // null roles/permissions claims become empty lists -> no authorities
        assertThat(principal.roles()).isEmpty();
        assertThat(principal.permissions()).isEmpty();
        assertThat(auth.getAuthorities()).isEmpty();
        // header must not be consulted once a cookie token is present
        verify(request, never()).getHeader(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token leaves the context empty but still proceeds down the chain")
    void expiredToken_notAuthenticated() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
        when(validator.isExpired("expired.token")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("invalid token (validator throws) clears the context and proceeds")
    void invalidToken_clearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer tampered.token");
        when(validator.isExpired("tampered.token")).thenReturn(false);
        when(validator.parseClaims("tampered.token")).thenThrow(new RuntimeException("bad signature"));

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("no token at all -> context untouched, chain proceeds, validator never called")
    void noToken_proceeds() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).isExpired(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-Bearer Authorization header is ignored")
    void nonBearerHeader_ignored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).isExpired(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("blank ACCESS_TOKEN cookie is skipped and the filter falls back to the header")
    void blankCookie_fallsBackToHeader() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "")});
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).isExpired(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("an already-authenticated context is left intact (filter does not re-parse)")
    void alreadyAuthenticated_skipped() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken("pre", "cred", List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer some.token");
        // isExpired/parseClaims may or may not be reached depending on ordering; keep lenient.
        lenient().when(validator.isExpired(anyString())).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isSameAs(existing);
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }

    // Mockito.mock helper kept local so the static import above is sufficient for collaborators.
    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
