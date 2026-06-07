package com.hieu.cart_service.interfaces.rest;

import com.hieu.cart_service.exception.CartItemNotFoundException;
import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import com.hieu.common.error.ErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Maps cart-service exceptions to stable JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(CartItemNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, ErrorCode.CART_NOT_FOUND.code(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> beanValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
            .toList();
        return body(HttpStatus.BAD_REQUEST, ErrorCode.CART_VALIDATION.code(), "Validation failed", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.CART_VALIDATION.code(), ex.getMessage(), req, null);
    }

    /** Malformed JSON body, missing required fields, primitive coercion failures → 400 not 500. */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> notReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest req) {
        var cause = ex.getMostSpecificCause();
        var msg = cause != null ? cause.getMessage() : ex.getMessage();
        return body(HttpStatus.BAD_REQUEST, ErrorCode.CART_VALIDATION.code(), "Malformed JSON: " + msg, req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, ErrorCode.CART_CONFLICT.code(), "Constraint violation", req, null);
    }

    // C1: Optimistic lock from @Version — concurrent writers hit this; client must retry.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> optimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, ErrorCode.CART_CONFLICT.code(), "Concurrent update, please retry", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, ErrorCode.CART_FORBIDDEN.code(), "Access denied", req, null);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> unauthenticated(AuthenticationCredentialsNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, ErrorCode.CART_UNAUTHORIZED.code(), "Authentication required", req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.CART_VALIDATION.code(), ex.getMessage(), req, null);
    }

    /**
     * Re-export ResponseStatusException with its declared status + reason. The
     * cart service uses this for "out of stock", "product deleted", etc. — the
     * FE relies on these status codes to choose the right toast.
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(
            org.springframework.web.server.ResponseStatusException ex, HttpServletRequest req) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        String code = switch (status) {
            case NOT_FOUND -> ErrorCode.CART_NOT_FOUND.code();
            case CONFLICT -> ErrorCode.CART_VALIDATION.code();
            default -> ErrorCode.CART_VALIDATION.code();
        };
        return body(status, code, reason, req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.CART_INTERNAL.code(), "Internal server error", req, null);
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
                                                        HttpServletRequest req, List<FieldError> fields) {
        return ResponseEntity.status(status).body(
            new ErrorResponse(code, message, req.getRequestURI(), Instant.now(), fields, null, Map.of()));
    }
}
