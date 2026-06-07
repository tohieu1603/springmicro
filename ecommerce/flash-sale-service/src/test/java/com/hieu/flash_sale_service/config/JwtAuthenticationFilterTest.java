package com.hieu.flash_sale_service.config;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter}. The {@link JwtTokenValidator} and servlet
 * objects are mocked; the {@link SecurityContextHolder} is the only real collaborator and is reset
 * after each test. Covers token extraction (header/cookie/prefix), happy-path principal mapping,
 * expired/invalid handling and the "already authenticated" short-circuit.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenValidator validator;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        filter = new JwtAuthenticationFilter(validator);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static Claims claims(String userId, String subject, List<String> roles, List<String> perms) {
        Claims c = org.mockito.Mockito.mock(Claims.class);
        lenient().when(c.get("userId", String.class)).thenReturn(userId);
        lenient().when(c.getSubject()).thenReturn(subject);
        lenient().when(c.get("roles")).thenReturn(roles);
        lenient().when(c.get("permissions")).thenReturn(perms);
        return c;
    }

    @Test
    @DisplayName("valid Bearer token sets an AuthenticatedUser principal with roles+permissions as authorities")
    void validBearer_setsAuthentication() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(validator.isExpired("good.token")).thenReturn(false);
        Claims parsed = claims("u-1", "alice", List.of("ROLE_ADMIN"), List.of("flash:write"));
        when(validator.parseClaims("good.token")).thenReturn(parsed);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        var user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.userId()).isEqualTo("u-1");
        assertThat(user.username()).isEqualTo("alice");
        assertThat(auth.getCredentials()).isEqualTo("good.token");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "flash:write");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("ACCESS_TOKEN cookie is used even when the Authorization header is absent")
    void cookieToken_used() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie.tok")});
        when(validator.isExpired("cookie.tok")).thenReturn(false);
        Claims parsed = claims("u-2", "bob", List.of("ROLE_USER"), List.of());
        when(validator.parseClaims("cookie.tok")).thenReturn(parsed);

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).userId()).isEqualTo("u-2");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("cookie takes precedence over the Authorization header")
    void cookiePrecedesHeader() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie.tok")});
        when(validator.isExpired("cookie.tok")).thenReturn(false);
        Claims parsed = claims("u-cookie", "c", List.of(), List.of());
        when(validator.parseClaims("cookie.tok")).thenReturn(parsed);

        filter.doFilterInternal(request, response, chain);

        verify(validator, never()).isExpired("header.tok");
        verify(validator).parseClaims("cookie.tok");
    }

    @Test
    @DisplayName("blank cookie value falls through to the Authorization header")
    void blankCookie_fallsToHeader() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "")});
        when(request.getHeader("Authorization")).thenReturn("Bearer header.tok");
        when(validator.isExpired("header.tok")).thenReturn(false);
        Claims parsed = claims("u-h", "h", List.of(), List.of());
        when(validator.parseClaims("header.tok")).thenReturn(parsed);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(validator).parseClaims("header.tok");
    }

    @Test
    @DisplayName("no token at all leaves the context unauthenticated and still calls the chain")
    void noToken_passThrough() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verify(validator, never()).parseClaims(anyString());
    }

    @Test
    @DisplayName("header without the Bearer prefix is ignored")
    void nonBearerHeader_ignored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token is not authenticated but the chain still proceeds")
    void expiredToken_notAuthenticated() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.tok");
        when(validator.isExpired("expired.tok")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("a parse failure clears the context and does not block the chain")
    void parseFailure_clearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.tok");
        when(validator.isExpired("bad.tok")).thenReturn(false);
        when(validator.parseClaims("bad.tok")).thenThrow(new JwtException("tampered"));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("an already-authenticated context is left untouched (no re-parse)")
    void alreadyAuthenticated_skips() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken("preset", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }
}
