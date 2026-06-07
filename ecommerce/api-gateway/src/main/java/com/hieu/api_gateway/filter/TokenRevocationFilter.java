package com.hieu.api_gateway.filter;

import com.hieu.api_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Checks every incoming JWT against the access-token blacklist stored in Redis.
 *
 * <p>Keys follow the pattern {@code "blacklist:{jti}"} written by auth-service when a
 * token is revoked (logout, password change, admin revoke). The filter runs after
 * {@link LoggingFilter} (order {@link Ordered#HIGHEST_PRECEDENCE} + 1) so the access-log
 * entry always reflects the revocation outcome.
 *
 * <p><b>Fail-open policy</b>: if Redis is unavailable the request is allowed through with
 * a warning. Access tokens live at most 15 minutes by default, so the worst-case exposure
 * of a revoked token during a Redis outage is bounded. Blocking every request on Redis
 * downtime would cause a full platform outage — which is worse than the compromise.
 */
@Slf4j
@Component
public class TokenRevocationFilter implements GlobalFilter, Ordered {

    /** Must match the prefix auth-service writes in {@code TokenBlacklistService}. */
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public TokenRevocationFilter(ReactiveStringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractBearer(exchange.getRequest());
        if (token == null) {
            // Public routes have no bearer token; nothing to check.
            return chain.filter(exchange);
        }

        String jti;
        try {
            jti = jwtUtil.extractTokenId(token);
        } catch (Exception e) {
            // Malformed / signature-failed tokens are rejected by the per-route JWT filter.
            // Just pass through so the proper filter produces the 401.
            log.debug("Could not extract jti (malformed token?); deferring to JwtAuthenticationFilter");
            return chain.filter(exchange);
        }

        if (jti == null) {
            // Legacy tokens without jti — blacklist couldn't contain them; safe to pass.
            return chain.filter(exchange);
        }

        return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti)
                .defaultIfEmpty(false)
                .onErrorResume(ex -> {
                    log.warn("Redis unavailable for blacklist check (jti={}); failing open: {}",
                            jti, ex.getMessage());
                    return Mono.just(false);
                })
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        log.warn("Rejected revoked token jti={}", jti);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    /** Runs early (right after LoggingFilter) so downstream filters already know the token is usable. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * Reads the access token from the ACCESS_TOKEN cookie first, Bearer header second.
     * Must match {@link JwtAuthenticationFilter}'s lookup order so blacklist check
     * sees the same token the downstream filter would accept.
     */
    private static String extractBearer(ServerHttpRequest request) {
        org.springframework.http.HttpCookie cookie = request.getCookies().getFirst("ACCESS_TOKEN");
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
