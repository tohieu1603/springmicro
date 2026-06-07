package com.hieu.order_service.infrastructure.security;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests for token extraction + SecurityContext population logic. */
@ExtendWith(MockitoExtension.class)
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
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void stubValidClaims() {
        when(validator.isExpired("tok")).thenReturn(false);
        when(validator.parseClaims("tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("user-1");
        when(claims.getSubject()).thenReturn("john");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));
        when(claims.get("permissions")).thenReturn(List.of("orders:read"));
    }

    @Test
    @DisplayName("valid Bearer header authenticates and builds the principal + authorities")
    void validHeader_authenticates() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer tok");
        stubValidClaims();

        filter().doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        var user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.userId()).isEqualTo("user-1");
        assertThat(user.username()).isEqualTo("john");
        assertThat(user.roles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
        assertThat(auth.getAuthorities())
                .extracting(a -> ((SimpleGrantedAuthority) a).getAuthority())
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "orders:read");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("access cookie is preferred and authenticates the request")
    void cookieToken_authenticates() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "tok")});
        stubValidClaims();

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("no token leaves the context empty but still proceeds")
    void noToken_skips() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token is rejected and no authentication is set")
    void expiredToken_rejected() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer tok");
        when(validator.isExpired("tok")).thenReturn(true);

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("parse failure clears the context and still proceeds down the chain")
    void parseFailure_clearsContext() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer tok");
        when(validator.isExpired("tok")).thenReturn(false);
        when(validator.parseClaims("tok")).thenThrow(new RuntimeException("tampered"));

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("header without the Bearer prefix is ignored")
    void nonBearerHeader_ignored() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter().doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-list role/permission claims collapse to empty lists")
    void missingClaimLists_defaultEmpty() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer tok");
        when(validator.isExpired("tok")).thenReturn(false);
        when(validator.parseClaims("tok")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("user-1");
        when(claims.getSubject()).thenReturn("john");
        lenient().when(claims.get("roles")).thenReturn(null);
        lenient().when(claims.get("permissions")).thenReturn("not-a-list");

        filter().doFilterInternal(request, response, chain);

        var user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(user.roles()).isEmpty();
        assertThat(user.permissions()).isEmpty();
    }
}
