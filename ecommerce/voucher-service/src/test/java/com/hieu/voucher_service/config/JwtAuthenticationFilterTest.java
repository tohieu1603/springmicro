package com.hieu.voucher_service.config;

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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter}. The {@link JwtTokenValidator}
 * is mocked (JWT parsing is exercised in its own tests); we verify only the filter's
 * own branching: token extraction (Bearer header vs ACCESS_TOKEN cookie), principal
 * population, expired/invalid handling, and that the chain always continues.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenValidator validator;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(validator);
    }

    @Test
    @DisplayName("Valid Bearer token sets AuthenticatedUser principal with role+permission authorities")
    void validBearerTokenAuthenticates() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("user-123");
        when(claims.getSubject()).thenReturn("john");
        when(claims.get("roles")).thenReturn(List.of("ROLE_ADMIN"));
        when(claims.get("permissions")).thenReturn(List.of("voucher:write"));

        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(validator.isExpired("good.token")).thenReturn(false);
        when(validator.parseClaims("good.token")).thenReturn(claims);

        filter().doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.userId()).isEqualTo("user-123");
        assertThat(user.username()).isEqualTo("john");
        assertThat(user.roles()).containsExactly("ROLE_ADMIN");
        assertThat(user.permissions()).containsExactly("voucher:write");
        assertThat(auth.getCredentials()).isEqualTo("good.token");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "voucher:write");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token from ACCESS_TOKEN cookie is used when no Authorization header present")
    void cookieTokenAuthenticates() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("u-1");
        when(claims.getSubject()).thenReturn("alice");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
        when(claims.get("permissions")).thenReturn(null);

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie.jwt")});
        when(validator.isExpired("cookie.jwt")).thenReturn(false);
        when(validator.parseClaims("cookie.jwt")).thenReturn(claims);

        filter().doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.roles()).containsExactly("ROLE_USER");
        assertThat(user.permissions()).isEmpty();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Cookie token is preferred over Bearer header when both present")
    void cookiePreferredOverHeader() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("u-2");
        when(claims.getSubject()).thenReturn("bob");
        when(claims.get("roles")).thenReturn(List.of());
        when(claims.get("permissions")).thenReturn(List.of());

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "from.cookie")});
        when(validator.isExpired("from.cookie")).thenReturn(false);
        when(validator.parseClaims("from.cookie")).thenReturn(claims);

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(validator).isExpired("from.cookie");
        verify(validator).parseClaims("from.cookie");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Blank cookie value falls back to Bearer header token")
    void blankCookieFallsBackToHeader() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("u-3");
        when(claims.getSubject()).thenReturn("carol");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
        when(claims.get("permissions")).thenReturn(List.of());

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "  ")});
        when(request.getHeader("Authorization")).thenReturn("Bearer header.token");
        when(validator.isExpired("header.token")).thenReturn(false);
        when(validator.parseClaims("header.token")).thenReturn(claims);

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(validator).parseClaims("header.token");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Expired token does not authenticate but still continues the chain")
    void expiredTokenSkips() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
        when(validator.isExpired("expired.token")).thenReturn(true);

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Validator throwing during parse clears context and continues the chain")
    void parseFailureClearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(validator.isExpired("bad.token")).thenReturn(false);
        when(validator.parseClaims("bad.token")).thenThrow(new RuntimeException("tampered"));

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("No token (no cookies, no header) leaves context unauthenticated and continues")
    void noTokenContinues() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).isExpired(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Non-Bearer Authorization header is ignored (no authentication)")
    void nonBearerHeaderIgnored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).isExpired(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Existing authentication in context is left untouched and chain continues")
    void existingAuthenticationNotOverwritten() throws Exception {
        Authentication preset = new UsernamePasswordAuthenticationToken("preexisting", "creds", List.of());
        SecurityContextHolder.getContext().setAuthentication(preset);

        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer some.token");

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(preset);
        verify(validator, never()).parseClaims(anyString());
        verify(chain).doFilter(request, response);
    }
}
