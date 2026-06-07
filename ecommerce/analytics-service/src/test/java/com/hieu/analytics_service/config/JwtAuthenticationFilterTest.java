package com.hieu.analytics_service.config;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
 * Pure unit test for the stateless JWT auth filter: mocks {@link JwtTokenValidator}
 * and uses Spring's servlet test doubles (no Spring context, no live deps).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter — Unit")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenValidator validator;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    private static final String TOKEN = "header.payload.signature";

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(validator);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubValidClaims(String userId, String subject,
                                 List<String> roles, List<String> permissions) {
        when(validator.isExpired(TOKEN)).thenReturn(false);
        when(validator.parseClaims(TOKEN)).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn(userId);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("roles")).thenReturn(roles);
        when(claims.get("permissions")).thenReturn(permissions);
    }

    @Test
    @DisplayName("Bearer header with valid token sets authenticated principal + authorities")
    void bearerHeader_validToken_setsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        stubValidClaims("u-1", "alice", List.of("ROLE_ADMIN"), List.of("perm.read"));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("u-1");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(auth.getCredentials()).isEqualTo(TOKEN);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "perm.read");
        // filter must always continue the chain
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("ACCESS_TOKEN cookie is used when no Authorization header present")
    void accessCookie_validToken_setsAuthentication() throws Exception {
        request.setCookies(new Cookie("ACCESS_TOKEN", TOKEN));
        stubValidClaims("u-2", "bob", List.of("ROLE_USER"), List.of());

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).username()).isEqualTo("bob");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("cookie takes precedence over Bearer header when both present")
    void cookiePrecedence_overHeader() throws Exception {
        request.setCookies(new Cookie("ACCESS_TOKEN", TOKEN));
        request.addHeader("Authorization", "Bearer other-token-value");
        stubValidClaims("u-3", "carol", List.of("ROLE_ADMIN"), List.of());

        filter.doFilterInternal(request, response, chain);

        // validator.parseClaims must have been called with the cookie token, not the header token
        verify(validator).parseClaims(TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("blank cookie value is ignored — falls through to (absent) header => no auth")
    void blankCookie_ignored_noAuth() throws Exception {
        request.setCookies(new Cookie("ACCESS_TOKEN", "   "));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
    }

    @Test
    @DisplayName("no token at all leaves SecurityContext empty and still continues the chain")
    void noToken_noAuthentication() throws Exception {
        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).isExpired(anyString());
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("header without Bearer prefix yields no token => no authentication")
    void headerWithoutBearerPrefix_noAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic " + TOKEN);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
    }

    @Test
    @DisplayName("Bearer with empty token after prefix is treated as no token")
    void bearerWithEmptyToken_noAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer    ");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
    }

    @Test
    @DisplayName("expired token is rejected — no authentication set, parseClaims not called")
    void expiredToken_rejected() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(validator.isExpired(TOKEN)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(validator, never()).parseClaims(anyString());
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("parse failure clears context and still continues the chain (no exception escapes)")
    void parseFailure_clearsContext() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(validator.isExpired(TOKEN)).thenReturn(false);
        when(validator.parseClaims(TOKEN)).thenThrow(new RuntimeException("tampered signature"));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("missing roles/permissions claims yield principal with empty authorities")
    void missingRolesAndPermissions_emptyAuthorities() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(validator.isExpired(TOKEN)).thenReturn(false);
        when(validator.parseClaims(TOKEN)).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-4");
        when(claims.getSubject()).thenReturn("dave");
        when(claims.get("roles")).thenReturn(null);
        when(claims.get("permissions")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("non-list roles claim is treated as empty (no ClassCastException)")
    void nonListRolesClaim_treatedAsEmpty() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(validator.isExpired(TOKEN)).thenReturn(false);
        when(validator.parseClaims(TOKEN)).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u-5");
        when(claims.getSubject()).thenReturn("erin");
        when(claims.get("roles")).thenReturn("ROLE_ADMIN"); // a String, not a List
        when(claims.get("permissions")).thenReturn(List.of("perm.x"));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("perm.x");
    }

    @Test
    @DisplayName("existing authentication in context is preserved — filter does not overwrite it")
    void existingAuthentication_notOverwritten() throws Exception {
        Authentication preset = new UsernamePasswordAuthenticationToken(
                "preset-user", "creds", List.of(new SimpleGrantedAuthority("ROLE_PRESET")));
        SecurityContextHolder.getContext().setAuthentication(preset);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        // validator must NOT be touched because authentication already exists
        lenient().when(validator.isExpired(anyString())).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(preset);
        verify(validator, never()).parseClaims(anyString());
    }
}
