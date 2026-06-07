package com.hieu.inventory_service.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}: each handler maps its exception
 * to a fixed HTTP status + stable {@link ErrorCode} and copies the request URI into the
 * body. No Spring context / MockMvc — the advice methods are plain methods returning a
 * {@link ResponseEntity}.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest req;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/inventory/test");
    }

    private static void assertBody(ResponseEntity<ErrorResponse> resp, HttpStatus status,
                                   String code, String path) {
        assertThat(resp.getStatusCode()).isEqualTo(status);
        ErrorResponse b = resp.getBody();
        assertThat(b).isNotNull();
        assertThat(b.code()).isEqualTo(code);
        assertThat(b.path()).isEqualTo(path);
        assertThat(b.timestamp()).isNotNull();
        assertThat(b.message()).isNotNull();
    }

    @Test
    @DisplayName("InventoryNotFoundException -> 404 INVENTORY-6004 with original message")
    void notFound() {
        var ex = new InventoryNotFoundException("42");
        var resp = handler.notFound(ex, req);
        assertBody(resp, HttpStatus.NOT_FOUND, ErrorCode.INVENTORY_NOT_FOUND.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("InsufficientStockException -> 409 INVENTORY-6009 with original message")
    void insufficientStock() {
        var ex = new InsufficientStockException("7", 2, 10);
        var resp = handler.insufficientStock(ex, req);
        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.INVENTORY_INSUFFICIENT.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 APP-400 carrying field errors")
    void beanValidation() {
        var binding = new BeanPropertyBindingResult(new Object(), "createInventoryRequest");
        binding.addError(new FieldError("createInventoryRequest", "sku", "rejected",
                false, null, null, "must not be blank"));
        var ex = new MethodArgumentNotValidException(null, binding);

        var resp = handler.beanValidation(ex, req);

        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("Validation failed");
        List<ErrorResponse.FieldError> fields = resp.getBody().fieldErrors();
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).field()).isEqualTo("sku");
        assertThat(fields.get(0).message()).isEqualTo("must not be blank");
        assertThat(fields.get(0).rejectedValue()).isEqualTo("rejected");
    }

    @Test
    @DisplayName("ConstraintViolationException -> 400 APP-400 with violation message")
    void constraintViolation() {
        var ex = new ConstraintViolationException("quantity must be >= 0", null);
        var resp = handler.constraintViolation(ex, req);
        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).contains("quantity must be >= 0");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 with 'Malformed JSON' prefix")
    void notReadable() {
        var ex = new HttpMessageNotReadableException("bad", new RuntimeException("Unexpected token"), null);
        var resp = handler.notReadable(ex, req);
        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).startsWith("Malformed JSON:");
        assertThat(resp.getBody().message()).contains("Unexpected token");
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 APP-409 'Constraint violation'")
    void dataIntegrity() {
        var ex = new DataIntegrityViolationException("dup", new RuntimeException("unique_sku"));
        var resp = handler.dataIntegrity(ex, req);
        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.APP_CONFLICT.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("Constraint violation");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 APP-403 'Access denied'")
    void accessDenied() {
        var resp = handler.accessDenied(new AccessDeniedException("nope"), req);
        assertBody(resp, HttpStatus.FORBIDDEN, ErrorCode.APP_FORBIDDEN.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AuthenticationCredentialsNotFoundException -> 401 APP-401 'Authentication required'")
    void unauthenticated() {
        var resp = handler.unauthenticated(new AuthenticationCredentialsNotFoundException("no creds"), req);
        assertBody(resp, HttpStatus.UNAUTHORIZED, ErrorCode.APP_UNAUTHORIZED.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 APP-400 with original message")
    void illegalArg() {
        var resp = handler.illegalArg(new IllegalArgumentException("delta too large"), req);
        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("delta too large");
    }

    @Test
    @DisplayName("IllegalStateException -> 422 APP-422 with original message")
    void illegalState() {
        var resp = handler.illegalState(new IllegalStateException("already confirmed"), req);
        assertBody(resp, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.APP_UNPROCESSABLE.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("already confirmed");
    }

    @Test
    @DisplayName("generic Exception -> 500 APP-500 with masked message")
    void unknown() {
        var resp = handler.unknown(new RuntimeException("boom with secrets"), req);
        assertBody(resp, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.APP_INTERNAL.code(),
                "/api/v1/inventory/test");
        assertThat(resp.getBody().message()).isEqualTo("Internal server error");
    }
}
