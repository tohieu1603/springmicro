package com.hieu.search_service.exception;

import com.hieu.common.api.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler} — each @ExceptionHandler method is
 * invoked directly with its exception and the returned status + {@link ApiResponse} body
 * (success flag, error code, message) are asserted. No MockMvc / Spring context.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleValidation joins field messages -> 400 VALIDATION_ERROR")
    void handleValidation_joinsFieldErrors() {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "name", "must not be blank"));
        binding.addError(new FieldError("req", "size", "must be <= 100"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).contains("must not be blank").contains("must be <= 100").contains(";");
    }

    @Test
    @DisplayName("handleValidation with no field errors -> empty message")
    void handleValidation_noFieldErrors() {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(resp.getBody().message()).isEmpty();
    }

    @Test
    @DisplayName("handleAccessDenied -> 403 FORBIDDEN with exception message")
    void handleAccessDenied() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleAccessDenied(new AccessDeniedException("nope"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isFalse();
        assertThat(resp.getBody().code()).isEqualTo("FORBIDDEN");
        assertThat(resp.getBody().message()).isEqualTo("nope");
    }

    @Test
    @DisplayName("handleBadArg -> 400 BAD_REQUEST with exception message")
    void handleBadArg() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleBadArg(new IllegalArgumentException("bad input"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(resp.getBody().message()).isEqualTo("bad input");
    }

    @Test
    @DisplayName("notReadable -> 400 BAD_REQUEST with generic 'Malformed JSON' message")
    void notReadable_withCause() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(new RuntimeException("Unexpected token"));

        ResponseEntity<ApiResponse<Void>> resp = handler.notReadable(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isFalse();
        assertThat(resp.getBody().code()).isEqualTo("BAD_REQUEST");
        // body never leaks parser internals
        assertThat(resp.getBody().message()).isEqualTo("Malformed JSON");
    }

    @Test
    @DisplayName("handleGeneral -> 500 INTERNAL_ERROR with safe generic message")
    void handleGeneral_hidesDetail() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleGeneral(new RuntimeException("DB password = secret"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isFalse();
        assertThat(resp.getBody().code()).isEqualTo("INTERNAL_ERROR");
        // does not leak the underlying exception message
        assertThat(resp.getBody().message()).isEqualTo("An unexpected error occurred");
    }
}
