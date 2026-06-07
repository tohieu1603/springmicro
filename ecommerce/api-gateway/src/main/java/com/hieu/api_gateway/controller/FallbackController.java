package com.hieu.api_gateway.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback endpoint wired into the Resilience4j circuit-breaker config.
 *
 * <p>When a downstream service trips a breaker, the gateway forwards the failed request to
 * {@code /fallback} and we return a well-formed {@link ApiResponse} error payload with
 * HTTP 503 so clients get a consistent shape instead of a naked stack trace.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping
    public ResponseEntity<ApiResponse<Void>> fallback() {
        log.warn("Circuit breaker fallback triggered — downstream service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        ErrorCode.SERVICE_UNAVAILABLE.code(),
                        "Service temporarily unavailable. Please try again later."));
    }
}
