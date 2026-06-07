package com.hieu.cart_service.interfaces.rest;

import com.hieu.cart_service.exception.CartItemNotFoundException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}: each handler is invoked directly with its
 * exception and the returned {@link ResponseEntity} is asserted on HTTP status, error code and body.
 * No MockMvc / Spring context — the advice is just a POJO whose methods we call.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/cart");
    }

    private static ErrorResponse bodyOf(ResponseEntity<ErrorResponse> resp, HttpStatus expected) {
        assertThat(resp.getStatusCode()).isEqualTo(expected);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path()).isEqualTo("/api/v1/cart");
        assertThat(body.timestamp()).isNotNull();
        return body;
    }

    @Test
    @DisplayName("CartItemNotFoundException -> 404 CART_NOT_FOUND with the exception message")
    void notFound() {
        var ex = new CartItemNotFoundException("u1", "100");

        var resp = handler.notFound(ex, req);

        var body = bodyOf(resp, HttpStatus.NOT_FOUND);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_NOT_FOUND.code());
        assertThat(body.message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 CART_VALIDATION and maps every field error")
    void beanValidation() throws NoSuchMethodException {
        var bindException = new BindException(new Object(), "target");
        bindException.addError(new FieldError("target", "quantity", 0, false, null, null, "must be >= 1"));
        var ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod("beanValidation"), -1),
                bindException);

        var resp = handler.beanValidation(ex, req);

        var body = bodyOf(resp, HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_VALIDATION.code());
        assertThat(body.message()).isEqualTo("Validation failed");
        assertThat(body.fieldErrors()).hasSize(1);
        assertThat(body.fieldErrors().get(0).field()).isEqualTo("quantity");
        assertThat(body.fieldErrors().get(0).message()).isEqualTo("must be >= 1");
        assertThat(body.fieldErrors().get(0).rejectedValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("ConstraintViolationException -> 400 CART_VALIDATION with the violation message")
    void constraintViolation() {
        var ex = new ConstraintViolationException("quantity must be positive", null);

        var resp = handler.constraintViolation(ex, req);

        var body = bodyOf(resp, HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_VALIDATION.code());
        assertThat(body.message()).contains("quantity must be positive");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 with 'Malformed JSON:' prefix from the root cause")
    void notReadable() {
        var inputMessage = mock(org.springframework.http.HttpInputMessage.class);
        var ex = new HttpMessageNotReadableException("bad", new IllegalStateException("unexpected token"), inputMessage);

        var resp = handler.notReadable(ex, req);

        var body = bodyOf(resp, HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_VALIDATION.code());
        assertThat(body.message()).startsWith("Malformed JSON:");
        assertThat(body.message()).contains("unexpected token");
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 CART_CONFLICT")
    void dataIntegrity() {
        var ex = new DataIntegrityViolationException("dup", new RuntimeException("unique key"));

        var resp = handler.dataIntegrity(ex, req);

        var body = bodyOf(resp, HttpStatus.CONFLICT);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_CONFLICT.code());
        assertThat(body.message()).isEqualTo("Constraint violation");
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException -> 409 CART_CONFLICT retry message")
    void optimisticLock() {
        var ex = new ObjectOptimisticLockingFailureException("CartItem", 1L);

        var resp = handler.optimisticLock(ex, req);

        var body = bodyOf(resp, HttpStatus.CONFLICT);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_CONFLICT.code());
        assertThat(body.message()).isEqualTo("Concurrent update, please retry");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 CART_FORBIDDEN")
    void accessDenied() {
        var resp = handler.accessDenied(new AccessDeniedException("nope"), req);

        var body = bodyOf(resp, HttpStatus.FORBIDDEN);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_FORBIDDEN.code());
        assertThat(body.message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AuthenticationCredentialsNotFoundException -> 401 CART_UNAUTHORIZED")
    void unauthenticated() {
        var resp = handler.unauthenticated(new AuthenticationCredentialsNotFoundException("no creds"), req);

        var body = bodyOf(resp, HttpStatus.UNAUTHORIZED);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_UNAUTHORIZED.code());
        assertThat(body.message()).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 CART_VALIDATION echoing the message")
    void illegalArg() {
        var resp = handler.illegalArg(new IllegalArgumentException("bad variantId"), req);

        var body = bodyOf(resp, HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_VALIDATION.code());
        assertThat(body.message()).isEqualTo("bad variantId");
    }

    @Test
    @DisplayName("ResponseStatusException(NOT_FOUND) -> 404 CART_NOT_FOUND with the declared reason")
    void responseStatus_notFound() {
        var ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "product deleted");

        var resp = handler.responseStatus(ex, req);

        var body = bodyOf(resp, HttpStatus.NOT_FOUND);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_NOT_FOUND.code());
        assertThat(body.message()).isEqualTo("product deleted");
    }

    @Test
    @DisplayName("ResponseStatusException(CONFLICT) -> 409 CART_VALIDATION code (per the switch)")
    void responseStatus_conflict() {
        var ex = new ResponseStatusException(HttpStatus.CONFLICT, "out of stock");

        var resp = handler.responseStatus(ex, req);

        var body = bodyOf(resp, HttpStatus.CONFLICT);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_VALIDATION.code());
        assertThat(body.message()).isEqualTo("out of stock");
    }

    @Test
    @DisplayName("ResponseStatusException without a reason -> falls back to the status reason phrase")
    void responseStatus_defaultReason() {
        var ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);

        var resp = handler.responseStatus(ex, req);

        var body = bodyOf(resp, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_VALIDATION.code());
        assertThat(body.message()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
    }

    @Test
    @DisplayName("Any other Exception -> 500 CART_INTERNAL with a generic message")
    void unknown() {
        var resp = handler.unknown(new RuntimeException("boom"), req);

        var body = bodyOf(resp, HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.code()).isEqualTo(ErrorCode.CART_INTERNAL.code());
        assertThat(body.message()).isEqualTo("Internal server error");
    }
}
