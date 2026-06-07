package com.hieu.api_gateway.filter;

import com.hieu.api_gateway.util.JwtUtil;
import com.hieu.common.security.AuthHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Per-route JWT authentication filter. Applied in {@code application.yaml} via
 * {@code filters: - name: JwtAuthenticationFilter} on protected routes.
 *
 * <p>The filter validates the Bearer token's signature + expiry, extracts identity
 * claims, and forwards them as {@code X-User-Id} / {@code X-User-Name} /
 * {@code X-Token-Id} / {@code X-Token-Version} headers. Downstream services trust
 * these headers because they come from a gateway-internal process that already
 * verified the signature.
 *
 * <p>Unauthenticated requests get a bare 401 (body is handled by the service's
 * own security chain once forwarded).
 */
@Slf4j
@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String token = readAccessToken(request);
            if (token == null) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing access token");
            }

            try {
                if (!jwtUtil.validateSignature(token) || jwtUtil.isExpired(token)) {
                    return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT");
                }

                ServerHttpRequest mutated = request.mutate()
                        .header(AuthHeaders.USER_ID,       nullSafe(jwtUtil.extractUserId(token)))
                        .header(AuthHeaders.USERNAME,      nullSafe(jwtUtil.extractUsername(token)))
                        .header(AuthHeaders.TOKEN_ID,      nullSafe(jwtUtil.extractTokenId(token)))
                        .header(AuthHeaders.TOKEN_VERSION, String.valueOf(jwtUtil.extractTokenVersion(token)))
                        .build();

                return chain.filter(exchange.mutate().request(mutated).build());

            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return reject(exchange, HttpStatus.UNAUTHORIZED, "JWT validation failed");
            }
        };
    }

    private static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason) {
        log.debug("Rejecting request: {}", reason);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    /**
     * Reads the access token from the {@code ACCESS_TOKEN} cookie first, then falls
     * back to the {@code Authorization: Bearer …} header for API clients.
     */
    private static String readAccessToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst("ACCESS_TOKEN");
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    /** Configuration placeholder — routes can pass settings here as the factory matures. */
    public static class Config {
    }
}
