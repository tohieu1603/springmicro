package com.hieu.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;

import com.hieu.api_gateway.util.JwtUtil;

import reactor.core.publisher.Mono;

/**
 * Key resolvers for Spring Cloud Gateway's {@code RequestRateLimiter} filter.
 *
 * <p>Two strategies are exposed:
 * <ul>
 *   <li><b>ipKeyResolver</b> (primary): throttles per client IP — safe default for
 *       unauthenticated endpoints like {@code /api/auth/login}.</li>
 *   <li><b>userKeyResolver</b>: extracts the user identity directly from the JWT so the
 *       key cannot be spoofed by a caller supplying a fake {@code X-User-Id} header.
 *       {@code X-User-Id} is stripped by the {@code RemoveRequestHeader} default-filter
 *       before it reaches this resolver anyway, so reading the header would always fall
 *       back to IP regardless of auth state. Falls back to IP when no valid token is present.</li>
 * </ul>
 *
 * <p>Routes select the resolver via {@code filters.RequestRateLimiter.key-resolver}.
 */
@Configuration
public class RateLimiterConfig {

    private final JwtUtil jwtUtil;

    public RateLimiterConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** Default IP-based limiter. */
    @Primary
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remote = exchange.getRequest().getRemoteAddress();
            String key = remote == null ? "unknown" : remote.getAddress().getHostAddress();
            return Mono.just("ip:" + key);
        };
    }

    /**
     * User-based limiter: reads userId directly from the JWT (cookie first, then Bearer header).
     * This prevents a caller from injecting an arbitrary {@code X-User-Id} header to escape
     * their own rate-limit bucket or poison another user's bucket.
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String token = extractToken(exchange);
            if (token != null) {
                try {
                    if (!jwtUtil.isExpired(token) && jwtUtil.validateSignature(token)) {
                        String userId = jwtUtil.extractUserId(token);
                        if (userId != null && !userId.isBlank()) {
                            return Mono.just("user:" + userId);
                        }
                    }
                } catch (Exception ignored) {
                    // Malformed token — fall through to IP-based key
                }
            }
            var remote = exchange.getRequest().getRemoteAddress();
            String ip = remote == null ? "unknown" : remote.getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }

    private static String extractToken(org.springframework.web.server.ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("ACCESS_TOKEN");
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) return null;
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }
}
