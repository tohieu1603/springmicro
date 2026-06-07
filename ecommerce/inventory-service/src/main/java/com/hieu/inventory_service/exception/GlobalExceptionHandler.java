package com.hieu.inventory_service.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Maps domain exceptions to stable JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(InventoryNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, ErrorCode.INVENTORY_NOT_FOUND.code(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> insufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, ErrorCode.INVENTORY_INSUFFICIENT.code(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> beanValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
            .toList();
        return body(HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(), "Validation failed", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(), ex.getMessage(), req, null);
    }

    /** Malformed JSON body, missing required fields, primitive coercion failures → 400 not 500. */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> notReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest req) {
        var cause = ex.getMostSpecificCause();
        var msg = cause != null ? cause.getMessage() : ex.getMessage();
        return body(HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(), "Malformed JSON: " + msg, req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, ErrorCode.APP_CONFLICT.code(), "Constraint violation", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, ErrorCode.APP_FORBIDDEN.code(), "Access denied", req, null);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> unauthenticated(AuthenticationCredentialsNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, ErrorCode.APP_UNAUTHORIZED.code(), "Authentication required", req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> illegalState(IllegalStateException ex, HttpServletRequest req) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.APP_UNPROCESSABLE.code(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.APP_INTERNAL.code(), "Internal server error", req, null);
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
                                                       HttpServletRequest req, List<ErrorResponse.FieldError> fields) {
        return ResponseEntity.status(status).body(
            new ErrorResponse(code, message, req.getRequestURI(), Instant.now(), fields, null, Map.of()));
    }
}
