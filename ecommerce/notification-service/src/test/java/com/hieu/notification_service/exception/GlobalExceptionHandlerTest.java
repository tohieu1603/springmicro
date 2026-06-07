package com.hieu.notification_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}: each handler method is invoked
 * directly with its exception and the returned {@link ResponseEntity} status + error
 * body are asserted. No Spring context, no MockMvc.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<Map<String, Object>> resp) {
        return resp.getBody();
    }

    @Test
    @DisplayName("NotificationNotFoundException -> 404 with NOT_FOUND body")
    void notFound() {
        var resp = handler.handleNotFound(new NotificationNotFoundException("missing 42"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var b = body(resp);
        assertThat(b).containsEntry("status", 404)
                .containsEntry("error", HttpStatus.NOT_FOUND.getReasonPhrase())
                .containsEntry("message", "missing 42");
        assertThat(b.get("timestamp")).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 with FORBIDDEN body")
    void accessDenied() {
        var resp = handler.handleAccessDenied(new AccessDeniedException("not yours"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var b = body(resp);
        assertThat(b).containsEntry("status", 403)
                .containsEntry("error", HttpStatus.FORBIDDEN.getReasonPhrase())
                .containsEntry("message", "not yours");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 with BAD_REQUEST body")
    void badRequest() {
        var resp = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var b = body(resp);
        assertThat(b).containsEntry("status", 400)
                .containsEntry("error", HttpStatus.BAD_REQUEST.getReasonPhrase())
                .containsEntry("message", "bad input");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 aggregating field errors")
    void validation() {
        var binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "title", "must not be blank"));
        binding.addError(new FieldError("req", "userId", "must not be null"));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);

        var resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var b = body(resp);
        assertThat(b).containsEntry("status", 400);
        assertThat((String) b.get("message"))
                .startsWith("Validation failed")
                .contains("title: must not be blank")
                .contains("userId: must not be null");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 prefixed 'Malformed JSON' using most-specific cause")
    void notReadable() {
        var cause = new IllegalStateException("unexpected token");
        var ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(cause);

        var resp = handler.handleNotReadable(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var b = body(resp);
        assertThat(b).containsEntry("status", 400);
        assertThat((String) b.get("message")).isEqualTo("Malformed JSON: unexpected token");
    }

    @Test
    @DisplayName("generic Exception -> 500 with a masked message")
    void general() {
        var resp = handler.handleGeneral(new RuntimeException("internal stack details"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        var b = body(resp);
        assertThat(b).containsEntry("status", 500)
                .containsEntry("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .containsEntry("message", "An unexpected error occurred");
        // internal detail must not leak
        assertThat((String) b.get("message")).doesNotContain("internal stack details");
    }
}
