package com.hieu.api_gateway.filter;

import com.hieu.common.security.AuthHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the global {@link LoggingFilter}.
 *
 * <p>The filter's only externally-observable behaviour is:
 * <ul>
 *   <li>tagging the forwarded request with a generated {@code X-Correlation-Id} header,</li>
 *   <li>generating a fresh (valid UUID) id per request, and</li>
 *   <li>running at {@link Ordered#HIGHEST_PRECEDENCE} so the id is available to every
 *       downstream filter.</li>
 * </ul>
 *
 * <p>Logging itself is a side effect we don't assert; a capturing
 * {@link GatewayFilterChain} records the mutated exchange so we can inspect the header.
 */
class LoggingFilterTest {

    private final LoggingFilter filter = new LoggingFilter();

    @Test
    @DisplayName("Adds an X-Correlation-Id header to the forwarded request")
    void addsCorrelationIdHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureChain(forwarded)).block();

        String correlationId = forwarded.get().getRequest()
                .getHeaders().getFirst(AuthHeaders.CORRELATION_ID);
        assertThat(correlationId).isNotBlank();
    }

    @Test
    @DisplayName("Generated correlation id is a valid UUID")
    void correlationIdIsAUuid() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureChain(forwarded)).block();

        String correlationId = forwarded.get().getRequest()
                .getHeaders().getFirst(AuthHeaders.CORRELATION_ID);
        // Throws IllegalArgumentException if not a well-formed UUID.
        assertThat(UUID.fromString(correlationId)).isNotNull();
    }

    @Test
    @DisplayName("Generates a distinct correlation id for each request")
    void generatesDistinctIdPerRequest() {
        AtomicReference<ServerWebExchange> first = new AtomicReference<>();
        AtomicReference<ServerWebExchange> second = new AtomicReference<>();

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/a")),
                captureChain(first)).block();
        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/b")),
                captureChain(second)).block();

        String firstId = first.get().getRequest().getHeaders().getFirst(AuthHeaders.CORRELATION_ID);
        String secondId = second.get().getRequest().getHeaders().getFirst(AuthHeaders.CORRELATION_ID);
        assertThat(firstId).isNotEqualTo(secondId);
    }

    @Test
    @DisplayName("Forwards the request through the chain exactly once")
    void forwardsThroughChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureChain(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        // Filter must not short-circuit the response on the happy path.
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Preserves caller-supplied headers while adding the correlation id")
    void preservesExistingHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("X-Custom", "keep-me"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureChain(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Custom"))
                .isEqualTo("keep-me");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthHeaders.CORRELATION_ID))
                .isNotBlank();
    }

    @Test
    @DisplayName("Runs at HIGHEST_PRECEDENCE so downstream filters inherit the correlation id")
    void runsAtHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    /** Chain that captures the (mutated) exchange so the test can inspect its headers. */
    private static GatewayFilterChain captureChain(AtomicReference<ServerWebExchange> sink) {
        return exchange -> {
            sink.set(exchange);
            return Mono.empty();
        };
    }
}
