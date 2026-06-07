package com.hieu.search_service.config;

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
import org.junit.jupiter.api.Nested;
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
 * Pure unit tests for {@link JwtAuthenticationFilter}. The {@link JwtTokenValidator} and the
 * servlet collaborators are mocked; the filter's token-extraction (cookie vs Bearer header),
 * expired/invalid handling and claims -> {@link AuthenticatedUser} principal + authority mapping
 * are exercised against the real {@link SecurityContextHolder}. No Spring context / HTTP.
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

    private void stubValidClaims(Claims claims) {
        when(validator.isExpired(anyString())).thenReturn(false);
        when(validator.parseClaims(anyString())).thenReturn(claims);
    }

    @Nested
    @DisplayName("authenticates")
    class Authenticates {

        @Test
        @DisplayName("Bearer header token -> principal + role/permission authorities set")
        void bearerHeader_setsAuthentication() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.get("userId", String.class)).thenReturn("u1");
            when(claims.getSubject()).thenReturn("alice");
            when(claims.get("roles")).thenReturn(List.of("ROLE_ADMIN"));
            when(claims.get("permissions")).thenReturn(List.of("search:index"));
            stubValidClaims(claims);

            filter.doFilterInternal(request, response, chain);

            Authentication auth = currentAuth();
            assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
            AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
            assertThat(user.userId()).isEqualTo("u1");
            assertThat(user.username()).isEqualTo("alice");
            assertThat(user.roles()).containsExactly("ROLE_ADMIN");
            assertThat(user.permissions()).containsExactly("search:index");
            assertThat(auth.getCredentials()).isEqualTo("good.token");
            assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "search:index");
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("ACCESS_TOKEN cookie token -> authentication set without reading the header")
        void cookieToken_setsAuthentication() throws Exception {
            when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "cookie.tok")});
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.get("userId", String.class)).thenReturn("u2");
            when(claims.getSubject()).thenReturn("bob");
            when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
            when(claims.get("permissions")).thenReturn(null); // -> empty list
            stubValidClaims(claims);

            filter.doFilterInternal(request, response, chain);

            Authentication auth = currentAuth();
            assertThat(auth).isNotNull();
            AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
            assertThat(user.userId()).isEqualTo("u2");
            assertThat(user.permissions()).isEmpty();
            assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("non-list roles claim degrades to empty authorities (no crash)")
        void nonListRolesClaim_emptyAuthorities() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.get("userId", String.class)).thenReturn("u3");
            when(claims.getSubject()).thenReturn("carol");
            when(claims.get("roles")).thenReturn("not-a-list");
            when(claims.get("permissions")).thenReturn(null);
            stubValidClaims(claims);

            filter.doFilterInternal(request, response, chain);

            Authentication auth = currentAuth();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities()).isEmpty();
            verify(chain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("does not authenticate")
    class DoesNotAuthenticate {

        @Test
        @DisplayName("no cookies and no Authorization header -> context stays empty, chain proceeds")
        void noToken() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isNull();
            verify(validator, never()).parseClaims(anyString());
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("header without Bearer prefix -> ignored, no authentication")
        void wrongPrefix() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Basic abc");

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isNull();
            verify(validator, never()).parseClaims(anyString());
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer prefix with empty token -> treated as no token")
        void emptyBearerToken() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer    ");

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isNull();
            verify(validator, never()).parseClaims(anyString());
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("blank cookie value falls back and (no header) yields no authentication")
        void blankCookieFallsBack() throws Exception {
            when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("ACCESS_TOKEN", "")});
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isNull();
            verify(validator, never()).parseClaims(anyString());
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("expired token -> not authenticated, claims never parsed, chain proceeds")
        void expiredToken() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer expired.tok");
            when(validator.isExpired("expired.tok")).thenReturn(true);

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isNull();
            verify(validator, never()).parseClaims(anyString());
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("parseClaims throwing -> context cleared, chain still proceeds")
        void parseFailureClearsContext() throws Exception {
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer bad.tok");
            when(validator.isExpired("bad.tok")).thenReturn(false);
            when(validator.parseClaims("bad.tok")).thenThrow(new RuntimeException("tampered signature"));

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isNull();
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("pre-existing authentication is left untouched (token not re-parsed)")
        void existingAuthenticationPreserved() throws Exception {
            Authentication existing = new UsernamePasswordAuthenticationToken("preset", "x", List.of());
            SecurityContextHolder.getContext().setAuthentication(existing);
            lenient().when(request.getCookies()).thenReturn(null);
            lenient().when(request.getHeader("Authorization")).thenReturn("Bearer good.token");

            filter.doFilterInternal(request, response, chain);

            assertThat(currentAuth()).isSameAs(existing);
            verify(validator, never()).parseClaims(anyString());
            verify(chain).doFilter(request, response);
        }
    }
}
