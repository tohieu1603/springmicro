package com.hieu.notification_service.config;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the reactive {@link JwtAuthenticationFilter}. The
 * {@link JwtTokenValidator} collaborator is mocked; token extraction (cookie vs.
 * bearer header), the valid / expired / unparseable branches, and propagation of
 * the {@link AuthenticatedUser} principal into the reactive security context are
 * asserted via mock {@link ServerWebExchange}s and a capturing {@link WebFilterChain}.
 * No Spring context, no real JWT parsing beyond the mock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter (unit)")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenValidator validator;
    @Mock Claims claims;

    JwtAuthenticationFilter filter;

    /** Captures the security context propagated downstream (if any) and a "called" flag. */
    private final AtomicReference<Authentication> captured = new AtomicReference<>();
    private final AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);

    private final WebFilterChain capturingChain = exchange -> {
        chainCalled.set(true);
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(captured::set)
                .then();
    };

    @BeforeEach
    void setup() {
        filter = new JwtAuthenticationFilter(validator);
    }

    private static ServerWebExchange withHeader(String authorization) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notifications/my")
                        .header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private static ServerWebExchange withCookie(String value) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notifications/my")
                        .cookie(new org.springframework.http.HttpCookie("ACCESS_TOKEN", value)));
    }

    private void stubValidClaims(String userId, String subject, List<String> roles, List<String> perms) {
        when(validator.isExpired("good")).thenReturn(false);
        when(validator.parseClaims("good")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn(userId);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("roles")).thenReturn(roles);
        when(claims.get("permissions")).thenReturn(perms);
    }

    @Test
    @DisplayName("no token: chain proceeds with no security context")
    void noToken() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/notifications/my"));

        filter.filter(exchange, capturingChain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(captured.get()).isNull();
    }

    @Test
    @DisplayName("valid bearer token: principal published with role + permission authorities")
    void validBearerToken() {
        stubValidClaims("u1", "alice", List.of("ROLE_ADMIN"), List.of("notif:read"));
        var exchange = withHeader("Bearer good");

        filter.filter(exchange, capturingChain).block();

        assertThat(chainCalled.get()).isTrue();
        Authentication auth = captured.get();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        var principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("u1");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.roles()).containsExactly("ROLE_ADMIN");
        assertThat(principal.permissions()).containsExactly("notif:read");
        assertThat(auth.getCredentials()).isEqualTo("good");
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "notif:read");
    }

    @Test
    @DisplayName("cookie token takes precedence and is parsed")
    void cookieToken() {
        stubValidClaims("u9", "bob", List.of(), List.of());
        var exchange = withCookie("good");

        filter.filter(exchange, capturingChain).block();

        Authentication auth = captured.get();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).userId()).isEqualTo("u9");
    }

    @Test
    @DisplayName("expired token: chain proceeds unauthenticated (claims never parsed)")
    void expiredToken() {
        when(validator.isExpired("good")).thenReturn(true);
        var exchange = withHeader("Bearer good");

        filter.filter(exchange, capturingChain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(captured.get()).isNull();
    }

    @Test
    @DisplayName("unparseable token: validator throws -> chain proceeds unauthenticated")
    void unparseableToken() {
        when(validator.isExpired("good")).thenReturn(false);
        when(validator.parseClaims("good")).thenThrow(new RuntimeException("bad signature"));
        var exchange = withHeader("Bearer good");

        filter.filter(exchange, capturingChain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(captured.get()).isNull();
    }

    @Test
    @DisplayName("non-Bearer Authorization header is ignored (treated as no token)")
    void nonBearerHeaderIgnored() {
        var exchange = withHeader("Basic abc123");

        filter.filter(exchange, capturingChain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(captured.get()).isNull();
    }

    @Test
    @DisplayName("non-list roles/permissions claim degrades to empty authority lists")
    void nonListClaimsDegradeToEmpty() {
        when(validator.isExpired("good")).thenReturn(false);
        when(validator.parseClaims("good")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("u1");
        when(claims.getSubject()).thenReturn("alice");
        // roles/permissions are not Lists -> extractList must yield List.of()
        when(claims.get("roles")).thenReturn("ROLE_ADMIN");
        lenient().when(claims.get("permissions")).thenReturn(null);
        var exchange = withHeader("Bearer good");

        filter.filter(exchange, capturingChain).block();

        Authentication auth = captured.get();
        assertThat(auth).isNotNull();
        var principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.roles()).isEmpty();
        assertThat(principal.permissions()).isEmpty();
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
