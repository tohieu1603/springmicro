package com.hieu.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Machine-readable error payload returned by every service's {@code GlobalExceptionHandler}.
 *
 * <p>Mirrors {@link com.hieu.common.api.ApiResponse}'s shape so successful and failed
 * responses can be parsed by the same client code branching on {@code code}.
 *
 * @param code         stable {@link ErrorCode} value
 * @param message      human-readable message
 * @param path         request path that produced the error
 * @param timestamp    server-side instant
 * @param fieldErrors  optional list of field-level validation errors
 * @param traceId      correlation id from the incoming request
 * @param details      optional free-form debugging details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors,
        String traceId,
        Map<String, Object> details
) {
    /** Single validation violation on a specific request field. */
    public record FieldError(String field, String message, Object rejectedValue) {}

    /** Compact factory for typical error responses. */
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                errorCode.code(),
                message == null ? errorCode.name() : message,
                path,
                Instant.now(),
                null, null, null);
    }

    /** Factory for validation failures. */
    public static ErrorResponse validation(String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                ErrorCode.VALIDATION_FAILED.code(),
                "Request validation failed",
                path,
                Instant.now(),
                fieldErrors, null, null);
    }
}
