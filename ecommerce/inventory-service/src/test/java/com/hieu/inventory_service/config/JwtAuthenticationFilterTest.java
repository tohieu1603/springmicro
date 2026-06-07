package com.hieu.inventory_service.config;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link JwtAuthenticationFilter}: token extraction (cookie precedence
 * over Authorization header, Bearer-prefix / blank handling), the expired-token short
 * circuit, claims -> {@link AuthenticatedUser} authentication population, and the
 * parse-failure clears-context branch. {@link JwtTokenValidator} + the servlet objects are
 * mocked; the chain is always continued. SecurityContextHolder is reset around each test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenValidator validator;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;
    @Mock Claims claims;

    JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
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
    @DisplayName("valid Bearer header token populates the SecurityContext with roles + permissions")
    void validHeaderToken_setsAuthentication() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(validator.isExpired("good.token")).thenReturn(false);
        when(validator.parseClaims("good.token")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-1");
        when(claims.getSubject()).thenReturn("alice");
        when(claims.get("roles")).thenReturn(List.of("ADMIN"));
        when(claims.get("permissions")).thenReturn(List.of("inventory:write"));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = currentAuth();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("u-1");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(auth.getCredentials()).isEqualTo("good.token");
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ADMIN", "inventory:write");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("cookie token takes precedence over the Authorization header")
    void cookieTokenPreferredOverHeader() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie.tok")});
        // header must NOT be consulted when a valid cookie exists
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer header.tok");
        when(validator.isExpired("cookie.tok")).thenReturn(false);
        when(validator.parseClaims("cookie.tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-2");
        when(claims.getSubject()).thenReturn("bob");
        when(claims.get("roles")).thenReturn(List.of("USER"));
        when(claims.get("permissions")).thenReturn(null); // non-list -> empty

        filter.doFilterInternal(request, response, chain);

        Authentication auth = currentAuth();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).userId()).isEqualTo("u-2");
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("USER");
        verify(validator).parseClaims("cookie.tok");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token leaves the context unauthenticated but still continues the chain")
    void expiredToken_noAuthentication() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.tok");
        when(validator.isExpired("expired.tok")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).parseClaims(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("no cookie and no Bearer header -> no token, no authentication")
    void noToken_noAuthentication() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).isExpired(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-Bearer Authorization header is ignored")
    void nonBearerHeaderIgnored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).isExpired(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("blank cookie value is skipped and falls through to the header")
    void blankCookieFallsBackToHeader() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "")});
        when(request.getHeader("Authorization")).thenReturn("Bearer header.tok");
        when(validator.isExpired("header.tok")).thenReturn(false);
        when(validator.parseClaims("header.tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-3");
        when(claims.getSubject()).thenReturn("carol");
        when(claims.get("roles")).thenReturn(List.of());
        when(claims.get("permissions")).thenReturn(List.of());

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNotNull();
        verify(validator).parseClaims("header.tok");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("parse failure clears the context and still continues the chain")
    void parseFailure_clearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer broken.tok");
        when(validator.isExpired("broken.tok")).thenReturn(false);
        when(validator.parseClaims("broken.tok")).thenThrow(new RuntimeException("tampered signature"));

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("empty Bearer header (just the prefix) yields no token")
    void emptyBearerHeaderIgnored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        filter.doFilterInternal(request, response, chain);

        assertThat(currentAuth()).isNull();
        verify(validator, never()).isExpired(any());
        verify(chain).doFilter(request, response);
    }
}
