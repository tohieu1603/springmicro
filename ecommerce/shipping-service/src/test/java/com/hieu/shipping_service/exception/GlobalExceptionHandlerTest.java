package com.hieu.shipping_service.exception;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}: every handler method is invoked
 * directly with its exception and the returned {@link ResponseEntity} is asserted for
 * HTTP status + stable error code + path. No Spring, no MockMvc.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/shipments/1");
    }

    private static void assertBody(ResponseEntity<ErrorResponse> resp, HttpStatus status, String code) {
        assertThat(resp.getStatusCode()).isEqualTo(status);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(code);
        assertThat(resp.getBody().path()).isEqualTo("/api/v1/shipments/1");
        assertThat(resp.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("ShipmentNotFoundException -> 404 SHIPPING-7404")
    void notFound() {
        var resp = handler.notFound(new ShipmentNotFoundException("5"), req);
        assertBody(resp, HttpStatus.NOT_FOUND, ErrorCode.SHIPMENT_NOT_FOUND.code());
        assertThat(resp.getBody().message()).contains("5");
    }

    @Test
    @DisplayName("DuplicateShipmentException -> 409 SHIPPING-7409")
    void conflict() {
        var resp = handler.conflict(new DuplicateShipmentException("ORD-9"), req);
        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.SHIPMENT_DUPLICATE.code());
    }

    @Test
    @DisplayName("InvalidShipmentStateException -> 422 SHIPPING-7422")
    void invalidState() {
        var resp = handler.invalidState(new InvalidShipmentStateException("bad transition"), req);
        assertBody(resp, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.SHIPMENT_INVALID_STATE.code());
        assertThat(resp.getBody().message()).isEqualTo("bad transition");
    }

    @Test
    @DisplayName("ShipmentAccessDeniedException -> 403 SHIPPING-7403")
    void accessDenied() {
        var resp = handler.accessDenied(new ShipmentAccessDeniedException("3"), req);
        assertBody(resp, HttpStatus.FORBIDDEN, ErrorCode.SHIPPING_FORBIDDEN.code());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 APP-400 with field errors")
    void beanValidation() {
        BindingResult br = new BeanPropertyBindingResult(new Object(), "createShipmentRequest");
        br.rejectValue(null, "code", "Validation failed");
        br.addError(new org.springframework.validation.FieldError(
                "createShipmentRequest", "recipientPhone", "012", false, null, null,
                "must be a valid Vietnamese phone number"));
        var ex = new MethodArgumentNotValidException(null, br);

        var resp = handler.beanValidation(ex, req);

        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).isEqualTo("Validation failed");
        assertThat(resp.getBody().fieldErrors()).hasSize(1);
        var fe = resp.getBody().fieldErrors().get(0);
        assertThat(fe.field()).isEqualTo("recipientPhone");
        assertThat(fe.message()).isEqualTo("must be a valid Vietnamese phone number");
        assertThat(fe.rejectedValue()).isEqualTo("012");
    }

    @Test
    @DisplayName("ConstraintViolationException -> 400 APP-400 carries message")
    void constraintViolation() {
        var ex = new ConstraintViolationException("weight must be >= 1", null);
        var resp = handler.constraintViolation(ex, req);
        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).contains("weight must be >= 1");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 APP-400 prefixed Malformed JSON")
    void notReadable() {
        var ex = new HttpMessageNotReadableException("boom", new RuntimeException("unexpected token"), null);
        var resp = handler.notReadable(ex, req);
        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).startsWith("Malformed JSON:");
        assertThat(resp.getBody().message()).contains("unexpected token");
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 APP-409 generic message")
    void dataIntegrity() {
        var ex = new DataIntegrityViolationException("dup key", new RuntimeException("unique_violation"));
        var resp = handler.dataIntegrity(ex, req);
        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.APP_CONFLICT.code());
        assertThat(resp.getBody().message()).isEqualTo("Constraint violation");
    }

    @Test
    @DisplayName("Spring AccessDeniedException -> 403 APP-403")
    void springAccessDenied() {
        var resp = handler.springAccessDenied(new AccessDeniedException("nope"), req);
        assertBody(resp, HttpStatus.FORBIDDEN, ErrorCode.APP_FORBIDDEN.code());
        assertThat(resp.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AuthenticationCredentialsNotFoundException -> 401 APP-401")
    void unauthenticated() {
        var resp = handler.unauthenticated(new AuthenticationCredentialsNotFoundException("anon"), req);
        assertBody(resp, HttpStatus.UNAUTHORIZED, ErrorCode.APP_UNAUTHORIZED.code());
        assertThat(resp.getBody().message()).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 APP-400 carries message")
    void illegalArg() {
        var resp = handler.illegalArg(new IllegalArgumentException("Invalid carrier: FEDEX"), req);
        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).isEqualTo("Invalid carrier: FEDEX");
    }

    @Test
    @DisplayName("IllegalStateException -> 422 APP-422 carries message")
    void illegalState() {
        var resp = handler.illegalState(new IllegalStateException("terminal state"), req);
        assertBody(resp, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.APP_UNPROCESSABLE.code());
        assertThat(resp.getBody().message()).isEqualTo("terminal state");
    }

    @Test
    @DisplayName("Unhandled Exception -> 500 APP-500 generic message")
    void unknown() {
        var resp = handler.unknown(new RuntimeException("kaboom"), req);
        assertBody(resp, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.APP_INTERNAL.code());
        assertThat(resp.getBody().message()).isEqualTo("Internal server error");
    }
}
