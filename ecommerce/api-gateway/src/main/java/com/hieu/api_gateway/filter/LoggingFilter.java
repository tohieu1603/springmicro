package com.hieu.api_gateway.filter;

import com.hieu.common.security.AuthHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Tags every request with a correlation id and emits access-log style entries.
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so every subsequent filter (JWT,
 * blacklist, routing) inherits the generated correlation id. The id is also pushed
 * downstream via the {@code X-Correlation-Id} header so log aggregation can tie an
 * entire request chain to one id.
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = UUID.randomUUID().toString();
        long started = System.currentTimeMillis();

        log.info("[{}] {} {}", correlationId, request.getMethod(), request.getURI());

        ServerHttpRequest mutated = request.mutate()
                .header(AuthHeaders.CORRELATION_ID, correlationId)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build())
                .then(Mono.fromRunnable(() -> {
                    long elapsed = System.currentTimeMillis() - started;
                    log.info("[{}] {} {} -> {} ({}ms)",
                            correlationId,
                            request.getMethod(), request.getURI(),
                            exchange.getResponse().getStatusCode(),
                            elapsed);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
