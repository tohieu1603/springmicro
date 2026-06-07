package com.hieu.analytics_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test of the hand-written CORS policy produced by {@link CorsConfig#corsFilter(List)}.
 * The bean factory method holds real configuration logic, so we invoke it directly (no Spring
 * context) and assert the resulting {@link CorsConfiguration} attached to "/**".
 */
@DisplayName("CorsConfig — Unit")
class CorsConfigTest {

    private final CorsConfig config = new CorsConfig();

    private CorsConfiguration resolveConfigFor(CorsFilter filter, String path) throws Exception {
        Field sourceField = CorsFilter.class.getDeclaredField("configSource");
        sourceField.setAccessible(true);
        var source = (org.springframework.web.cors.CorsConfigurationSource) sourceField.get(filter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        return source.getCorsConfiguration(request);
    }

    @Test
    @DisplayName("registers the configuration under the /** path pattern")
    void registersUnderWildcardPath() throws Exception {
        CorsFilter filter = config.corsFilter(List.of("http://localhost:3000"));

        CorsConfiguration matched = resolveConfigFor(filter, "/api/v1/analytics/summary");

        assertThat(matched).isNotNull();
    }

    @Test
    @DisplayName("allowed origins come straight from the injected list")
    void allowedOrigins_fromInjectedList() throws Exception {
        List<String> origins = List.of("https://admin.example.com", "https://staging.example.com");
        CorsFilter filter = config.corsFilter(origins);

        CorsConfiguration cfg = resolveConfigFor(filter, "/anything");

        assertThat(cfg.getAllowedOrigins())
                .containsExactly("https://admin.example.com", "https://staging.example.com");
    }

    @Test
    @DisplayName("permits the expected HTTP methods including OPTIONS preflight")
    void allowedMethods_includePreflight() throws Exception {
        CorsConfiguration cfg = resolveConfigFor(config.corsFilter(List.of("http://x")), "/x");

        assertThat(cfg.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("allows Authorization/Content-Type/Accept/X-Requested-With headers")
    void allowedHeaders_configured() throws Exception {
        CorsConfiguration cfg = resolveConfigFor(config.corsFilter(List.of("http://x")), "/x");

        assertThat(cfg.getAllowedHeaders())
                .containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept", "X-Requested-With");
    }

    @Test
    @DisplayName("exposes the Authorization header to the browser")
    void exposedHeaders_authorization() throws Exception {
        CorsConfiguration cfg = resolveConfigFor(config.corsFilter(List.of("http://x")), "/x");

        assertThat(cfg.getExposedHeaders()).containsExactly("Authorization");
    }

    @Test
    @DisplayName("allows credentials and sets a one-hour preflight max-age")
    void credentialsAndMaxAge() throws Exception {
        CorsConfiguration cfg = resolveConfigFor(config.corsFilter(List.of("http://x")), "/x");

        assertThat(cfg.getAllowCredentials()).isTrue();
        assertThat(cfg.getMaxAge()).isEqualTo(3600L);
    }
}
