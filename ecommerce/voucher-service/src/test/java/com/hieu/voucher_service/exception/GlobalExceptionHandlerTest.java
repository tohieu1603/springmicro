package com.hieu.voucher_service.exception;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler} — each handler method is
 * invoked directly with its exception; we assert the HTTP status and the
 * {@link ApiResponse} error code/message body. No Spring context, no MockMvc.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("VoucherNotFoundException -> 404 NOT_FOUND with VOUCHER_NOT_FOUND code")
    void handleNotFound() {
        var ex = new VoucherNotFoundException("SAVE10");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiResponse<Void> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo(ErrorCode.VOUCHER_NOT_FOUND.code());
        assertThat(body.message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("VoucherExpiredException -> 422 UNPROCESSABLE_ENTITY with VOUCHER_REJECTED code")
    void handleExpired() {
        var ex = new VoucherExpiredException("EXPIRED1");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleExpired(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VOUCHER_REJECTED.code());
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("VoucherUsageLimitException -> 409 CONFLICT with VOUCHER_LIMIT_REACHED code")
    void handleUsageLimit() {
        var ex = new VoucherUsageLimitException("LIMITED");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUsageLimit(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VOUCHER_LIMIT_REACHED.code());
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("VoucherMinOrderException -> 422 UNPROCESSABLE_ENTITY with VOUCHER_REJECTED code")
    void handleMinOrder() {
        var ex = new VoucherMinOrderException("MIN50", BigDecimal.valueOf(50));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleMinOrder(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VOUCHER_REJECTED.code());
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("VoucherInactiveException -> 422 UNPROCESSABLE_ENTITY with VOUCHER_REJECTED code")
    void handleInactive() {
        var ex = new VoucherInactiveException("OFF");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleInactive(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VOUCHER_REJECTED.code());
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("DuplicateVoucherException -> 409 CONFLICT with VOUCHER_LIMIT_REACHED code")
    void handleDuplicate() {
        var ex = new DuplicateVoucherException("DUP");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDuplicate(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VOUCHER_LIMIT_REACHED.code());
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("AuthorizationDeniedException -> 403 FORBIDDEN with generic 'Access denied' message")
    void handleAuthorizationDenied() {
        var ex = new AuthorizationDeniedException("denied");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleAuthorizationDenied(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.FORBIDDEN.code());
        assertThat(resp.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 BAD_REQUEST with VALIDATION_FAILED code and original message")
    void handleIllegalArgument() {
        var ex = new IllegalArgumentException("bad input");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArgument(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().message()).isEqualTo("bad input");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 BAD_REQUEST joining field errors")
    void handleValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("req", "code", "must not be blank"),
                new FieldError("req", "orderAmount", "must be positive")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().message())
                .contains("code: must not be blank")
                .contains("orderAmount: must be positive")
                .contains(", ");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 BAD_REQUEST with APP_BAD_REQUEST code and 'Malformed JSON'")
    void handleNotReadable() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("boom", new RuntimeException("root cause"), null);

        ResponseEntity<ApiResponse<Void>> resp = handler.notReadable(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).isEqualTo("Malformed JSON");
    }

    @Test
    @DisplayName("Generic Exception -> 500 INTERNAL_SERVER_ERROR with INTERNAL_ERROR code and generic message")
    void handleGeneric() {
        var ex = new RuntimeException("unexpected");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.code());
        assertThat(resp.getBody().message()).isEqualTo("Internal server error");
    }
}
