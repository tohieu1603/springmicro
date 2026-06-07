package com.hieu.user_profile_service.config;

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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter}: token extraction (cookie precedence,
 * Bearer header, blank/malformed handling), expiry rejection, principal population from claims,
 * and exception-driven context clearing. {@link JwtTokenValidator} and {@link Claims} are
 * mocked (no signing key, no HTTP); only the {@link SecurityContextHolder} thread-local is used.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenValidator validator;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    JwtAuthenticationFilter filter;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        filter = new JwtAuthenticationFilter(validator);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Claims claims(String userId, String subject, List<String> roles, List<String> perms) {
        Claims c = org.mockito.Mockito.mock(Claims.class);
        lenient().when(c.get("userId", String.class)).thenReturn(userId);
        lenient().when(c.getSubject()).thenReturn(subject);
        lenient().when(c.get("roles")).thenReturn(roles);
        lenient().when(c.get("permissions")).thenReturn(perms);
        return c;
    }

    @Test
    @DisplayName("valid Bearer token sets an authenticated principal with roles+permissions as authorities")
    void validHeaderToken_setsPrincipal() throws Exception {
        Claims c = claims("u-1", "alice", List.of("ROLE_USER"), List.of("READ"));
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(validator.isExpired("good-token")).thenReturn(false);
        when(validator.parseClaims("good-token")).thenReturn(c);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("u-1");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(auth.getCredentials()).isEqualTo("good-token");
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_USER", "READ");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("ACCESS_TOKEN cookie takes precedence over the Authorization header")
    void cookieTakesPrecedence() throws Exception {
        Claims c = claims("u-2", "bob", List.of("ROLE_ADMIN"), List.of());
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("ACCESS_TOKEN", "cookie-token") });
        when(validator.isExpired("cookie-token")).thenReturn(false);
        when(validator.parseClaims("cookie-token")).thenReturn(c);

        filter.doFilterInternal(request, response, chain);

        AuthenticatedUser principal =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo("u-2");
        // header never consulted because the cookie supplied a token
        verify(request, never()).getHeader(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token leaves the context unauthenticated but still continues the chain")
    void expiredToken_noPrincipal() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer stale");
        when(validator.isExpired("stale")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("no token (no cookie, no Bearer header) -> no authentication, chain continues")
    void noToken_noPrincipal() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).isExpired(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-Bearer Authorization header is ignored")
    void nonBearerHeader_ignored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).isExpired(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("blank ACCESS_TOKEN cookie is skipped and the Bearer header is used instead")
    void blankCookie_fallsBackToHeader() throws Exception {
        Claims c = claims("u-3", "carol", List.of("ROLE_USER"), List.of());
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("ACCESS_TOKEN", "   ") });
        when(request.getHeader("Authorization")).thenReturn("Bearer hdr-token");
        when(validator.isExpired("hdr-token")).thenReturn(false);
        when(validator.parseClaims("hdr-token")).thenReturn(c);

        filter.doFilterInternal(request, response, chain);

        AuthenticatedUser principal =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo("u-3");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("parse failure clears the context and still continues the chain")
    void parseThrows_clearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer broken");
        when(validator.isExpired("broken")).thenReturn(false);
        when(validator.parseClaims("broken")).thenThrow(new RuntimeException("bad signature"));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-list roles/permissions claims default to empty authorities")
    void nonListClaims_emptyAuthorities() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(validator.isExpired("t")).thenReturn(false);
        Claims c = org.mockito.Mockito.mock(Claims.class);
        lenient().when(c.get("userId", String.class)).thenReturn("u-9");
        lenient().when(c.getSubject()).thenReturn("dave");
        lenient().when(c.get("roles")).thenReturn("not-a-list");
        lenient().when(c.get("permissions")).thenReturn(null);
        when(validator.parseClaims("t")).thenReturn(c);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
        verify(chain).doFilter(request, response);
    }
}
