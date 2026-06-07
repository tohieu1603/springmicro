package com.hieu.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Minimal gateway security — the gateway relies on downstream services to enforce
 * authentication (they're reachable only via this edge). This configuration:
 *
 * <ul>
 *   <li>Provides a reactive {@link CorsConfigurationSource} shared by every route.</li>
 *   <li>Leaves request-level auth to {@code JwtAuthenticationFilter} + {@code TokenRevocationFilter}.</li>
 * </ul>
 *
 * <p>CORS origins come from {@code cors.allowed-origins} so environments can vary.
 */
@Configuration
public class GatewaySecurityConfig {

    /**
     * CORS source applied to every route. Origins are configurable; credentials are
     * enabled so cookies / Authorization headers can flow from browser clients.
     *
     * @return bean picked up by Spring Cloud Gateway's globalcors config
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
            List<String> allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();
        // Explicit origin list — wildcard "*" combined with allowCredentials=true is a
        // privacy leak (any origin can spend the user's cookies). Spec forbids the combo
        // but Spring's `setAllowedOriginPatterns("*")` sneaks past the browser check.
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "X-Requested-With", "X-Idempotency-Key", "X-Correlation-Id"));
        cfg.setExposedHeaders(List.of("Content-Type", "Authorization",
                "X-User-Id", "X-User-Name", "X-Token-Id", "X-Correlation-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
