package com.hieu.payment_service.security;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter} — token extraction precedence
 * (cookie then Bearer header), expired-token rejection, claim -> principal/authority
 * mapping into the SecurityContext, and the fail-safe context-clear on parse error.
 * The validator is mocked; no real crypto, no Spring context, no web layer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenValidator validator;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;
    @Mock Claims claims;

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(validator);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("no cookies and no Authorization header -> chain proceeds, no authentication")
    void noToken_passThrough() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter().doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(chain).doFilter(request, response);
        verify(validator, never()).parseClaims(anyString());
    }

    @Test
    @DisplayName("malformed Authorization header (no Bearer prefix) yields no token")
    void wrongHeaderPrefix_passThrough() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter().doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(chain).doFilter(request, response);
        verify(validator, never()).isExpired(anyString());
    }

    @Test
    @DisplayName("expired token is rejected: context stays empty, chain still proceeds")
    void expiredToken_rejected() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-tok");
        when(validator.isExpired("expired-tok")).thenReturn(true);

        filter().doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("valid Bearer token populates SecurityContext with user + role/permission authorities")
    void validBearer_authenticates() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good-tok");
        when(validator.isExpired("good-tok")).thenReturn(false);
        when(validator.parseClaims("good-tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("user-123");
        when(claims.getSubject()).thenReturn("alice");
        when(claims.get("roles")).thenReturn(List.of("ROLE_ADMIN"));
        when(claims.get("permissions")).thenReturn(List.of("payment:read"));

        filter().doFilterInternal(request, response, chain);

        Authentication auth = currentAuth();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getCredentials()).isEqualTo("good-tok");
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("user-123");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.roles()).containsExactly("ROLE_ADMIN");
        assertThat(principal.permissions()).containsExactly("payment:read");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "payment:read");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("ACCESS_TOKEN cookie takes precedence over header and authenticates")
    void cookieToken_authenticates() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie-tok")});
        when(validator.isExpired("cookie-tok")).thenReturn(false);
        when(validator.parseClaims("cookie-tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-9");
        when(claims.getSubject()).thenReturn("bob");
        when(claims.get("roles")).thenReturn(null);          // absent roles -> empty list
        when(claims.get("permissions")).thenReturn("not-a-list"); // non-list -> empty list

        filter().doFilterInternal(request, response, chain);

        Authentication auth = currentAuth();
        assertThat(auth).isNotNull();
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.roles()).isEmpty();
        assertThat(principal.permissions()).isEmpty();
        assertThat(auth.getAuthorities()).isEmpty();
        // header must not be consulted when a usable cookie is present
        verify(request, never()).getHeader(anyString());
    }

    @Test
    @DisplayName("blank cookie value falls back to the Bearer header")
    void blankCookie_fallsBackToHeader() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "")});
        when(request.getHeader("Authorization")).thenReturn("Bearer header-tok");
        when(validator.isExpired("header-tok")).thenReturn(false);
        when(validator.parseClaims("header-tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-1");
        when(claims.getSubject()).thenReturn("carol");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
        when(claims.get("permissions")).thenReturn(List.of());

        filter().doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNotNull();
        verify(validator).parseClaims("header-tok");
    }

    @Test
    @DisplayName("parse failure clears the context and lets the chain proceed (fail-open to anonymous)")
    void parseFailure_clearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer tampered");
        when(validator.isExpired("tampered")).thenReturn(false);
        when(validator.parseClaims("tampered")).thenThrow(new RuntimeException("bad signature"));

        filter().doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("existing authentication is left untouched (filter is idempotent)")
    void existingAuth_notOverwritten() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken(
                "preset", "creds", List.of(new SimpleGrantedAuthority("ROLE_X")));
        SecurityContextHolder.getContext().setAuthentication(existing);
        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer good-tok");

        filter().doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isSameAs(existing);
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }
}
