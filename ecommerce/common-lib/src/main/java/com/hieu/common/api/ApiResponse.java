package com.hieu.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Unified envelope for every REST response in the platform.
 *
 * <p>Immutable record — generates readable JSON and lets services compose
 * {@code ApiResponse<SomeDto>} without Lombok builders. {@code null} fields are
 * omitted from the wire format so successful responses don't carry empty error
 * slots (and vice versa).
 *
 * @param success   {@code true} for 2xx responses, {@code false} for handled errors
 * @param code      stable machine-readable code ({@code "OK"} or {@code "AUTH-1001"}, …)
 * @param message   optional human-readable message
 * @param data      payload (present when {@code success=true})
 * @param timestamp server-side instant the response was built
 * @param traceId   correlation id from the incoming request, if present
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp,
        String traceId
) {
    /** Success response with only a payload. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", null, data, Instant.now(), null);
    }

    /** Success response with a human-readable message + payload. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, "OK", message, data, Instant.now(), null);
    }

    /** Error response — pair with {@link com.hieu.common.error.ErrorCode}. */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now(), null);
    }

    /** Attaches a trace id to an existing response. */
    public ApiResponse<T> withTraceId(String traceId) {
        return new ApiResponse<>(success, code, message, data, timestamp, traceId);
    }
}
