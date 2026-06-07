package com.hieu.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration for browser-based clients.
 *
 * <p>Origins are supplied via the {@code cors.allowed-origins} property (comma-
 * separated) so prod/staging can differ from dev without code changes. Credentials are
 * enabled so cookies / Authorization headers can round-trip; wildcard origins are
 * therefore disallowed by the CORS spec and we fall back to the concrete list.
 *
 * <p>Exposed as a {@link CorsConfigurationSource} bean (not a standalone {@code CorsFilter})
 * so the Spring Security filter chain picks it up via {@code http.cors(...)} and correctly
 * short-circuits pre-flight {@code OPTIONS} requests before they hit the auth rules. A bare
 * {@code CorsFilter} bean ran <em>after</em> the security chain and let pre-flight 401.
 */
@Configuration
public class CorsConfig {

    /**
     * Builds the CORS policy applied to every endpoint.
     *
     * @param allowedOrigins comma-separated origins from {@code cors.allowed-origins}
     * @return configured CORS source consumed by Spring Security
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
            List<String> allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
