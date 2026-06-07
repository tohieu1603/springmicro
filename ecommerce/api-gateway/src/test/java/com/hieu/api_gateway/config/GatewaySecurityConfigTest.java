package com.hieu.api_gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the gateway CORS configuration factory.
 *
 * <p>{@code corsConfigurationSource} is a pure factory: given the configured allowed
 * origins it builds a {@link CorsConfiguration} registered for every path. No Spring
 * context is needed — we invoke the {@code @Bean} method directly and resolve the
 * registered configuration through a {@link MockServerWebExchange}.
 *
 * <p>These assertions pin the security-relevant contract: an explicit (non-wildcard)
 * origin list paired with {@code allowCredentials=true}, the permitted methods/headers,
 * the exposed identity headers, and the preflight cache duration.
 */
class GatewaySecurityConfigTest {

    private final GatewaySecurityConfig config = new GatewaySecurityConfig();

    private static final List<String> ORIGINS =
            List.of("https://app.hieu.com", "https://admin.hieu.com");

    private CorsConfiguration resolved;

    @BeforeEach
    void setUp() {
        CorsConfigurationSource source = config.corsConfigurationSource(ORIGINS);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/anything"));
        resolved = source.getCorsConfiguration(exchange);
        assertThat(resolved).as("a CORS config must be registered for every path").isNotNull();
    }

    @Test
    @DisplayName("Uses the supplied explicit origin list (no wildcard)")
    void usesExplicitOrigins() {
        assertThat(resolved.getAllowedOrigins()).containsExactlyElementsOf(ORIGINS);
        assertThat(resolved.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    @DisplayName("Allows credentials (cookies / Authorization header) to flow")
    void allowsCredentials() {
        assertThat(resolved.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("Permits the standard set of HTTP methods including preflight OPTIONS")
    void permitsExpectedMethods() {
        assertThat(resolved.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("Permits the headers browser clients send (Authorization, idempotency, correlation id)")
    void permitsExpectedRequestHeaders() {
        assertThat(resolved.getAllowedHeaders())
                .contains("Authorization", "Content-Type", "Accept",
                        "X-Requested-With", "X-Idempotency-Key", "X-Correlation-Id");
    }

    @Test
    @DisplayName("Exposes the gateway-injected identity headers to the browser")
    void exposesIdentityHeaders() {
        assertThat(resolved.getExposedHeaders())
                .contains("Content-Type", "Authorization",
                        "X-User-Id", "X-User-Name", "X-Token-Id", "X-Correlation-Id");
    }

    @Test
    @DisplayName("Caches preflight responses for one hour")
    void cachesPreflightForOneHour() {
        assertThat(resolved.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Registers the same configuration for every path under /**")
    void appliesToEveryPath() {
        CorsConfigurationSource source = config.corsConfigurationSource(ORIGINS);

        CorsConfiguration root = source.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/")));
        CorsConfiguration deep = source.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/42/items")));

        assertThat(root).isNotNull();
        assertThat(deep).isNotNull();
        assertThat(deep.getAllowedOrigins()).containsExactlyElementsOf(ORIGINS);
    }
}
