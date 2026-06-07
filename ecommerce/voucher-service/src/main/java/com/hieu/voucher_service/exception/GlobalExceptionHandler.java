package com.hieu.voucher_service.exception;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VoucherNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(VoucherNotFoundException ex) {
        log.warn("Voucher not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.VOUCHER_NOT_FOUND.code(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpired(VoucherExpiredException ex) {
        log.warn("Voucher expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ErrorCode.VOUCHER_REJECTED.code(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsageLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsageLimit(VoucherUsageLimitException ex) {
        log.warn("Voucher usage limit: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCode.VOUCHER_LIMIT_REACHED.code(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherMinOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMinOrder(VoucherMinOrderException ex) {
        log.warn("Voucher min order not met: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ErrorCode.VOUCHER_REJECTED.code(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleInactive(VoucherInactiveException ex) {
        log.warn("Voucher inactive: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ErrorCode.VOUCHER_REJECTED.code(), ex.getMessage()));
    }

    @ExceptionHandler(DuplicateVoucherException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateVoucherException ex) {
        log.warn("Duplicate voucher: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCode.VOUCHER_LIMIT_REACHED.code(), ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.code(), "Access denied"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.code(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.code(), message));
    }

    /** Malformed JSON body, missing required fields, primitive coercion failures → 400 not 500. */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        var cause = ex.getMostSpecificCause();
        log.warn("Malformed JSON: {}", cause != null ? cause.getMessage() : ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.APP_BAD_REQUEST.code(), "Malformed JSON"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), "Internal server error"));
    }
}
