package com.hieu.order_service.interfaces.rest;

import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import com.hieu.order_service.application.common.ApplicationException;
import com.hieu.order_service.application.common.ValidationException;
import com.hieu.order_service.domain.exception.*;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests mapping each exception to its HTTP status, error code and body. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/orders/1");
    }

    private ErrorResponse body(ResponseEntity<ErrorResponse> resp, HttpStatus expected) {
        assertThat(resp.getStatusCode()).isEqualTo(expected);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().path()).isEqualTo("/api/v1/orders/1");
        assertThat(resp.getBody().timestamp()).isNotNull();
        return resp.getBody();
    }

    @Test
    @DisplayName("OrderNotFoundException -> 404 with domain code")
    void notFound_order() {
        var ex = new OrderNotFoundException(7L);
        var b = body(handler.notFound(ex, req), HttpStatus.NOT_FOUND);
        assertThat(b.code()).isEqualTo(ErrorCode.ORDER_NOT_FOUND.code());
        assertThat(b.message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("ReturnRequestNotFoundException -> 404")
    void notFound_returnRequest() {
        var b = body(handler.notFound(new ReturnRequestNotFoundException(3L), req), HttpStatus.NOT_FOUND);
        assertThat(b.code()).isEqualTo(ErrorCode.RETURN_REQUEST_NOT_FOUND.code());
    }

    @Test
    @DisplayName("DuplicateOrderException -> 409")
    void conflict() {
        var b = body(handler.conflict(new DuplicateOrderException("k"), req), HttpStatus.CONFLICT);
        assertThat(b.code()).isEqualTo(ErrorCode.ORDER_DUPLICATE.code());
    }

    @Test
    @DisplayName("InvalidOrderStateException -> 422")
    void invalidState() {
        var b = body(handler.invalidState(new InvalidOrderStateException("bad"), req), HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(b.code()).isEqualTo(ErrorCode.ORDER_INVALID_STATE.code());
    }

    @Test
    @DisplayName("InsufficientStockException -> 400")
    void badRequest_stock() {
        var b = body(handler.badRequest(new InsufficientStockException("no stock"), req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo(ErrorCode.ORDER_INSUFFICIENT_STOCK.code());
    }

    @Test
    @DisplayName("EmptyCartException -> 400")
    void badRequest_emptyCart() {
        var b = body(handler.badRequest(new EmptyCartException("u"), req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo(ErrorCode.ORDER_EMPTY_CART.code());
    }

    @Test
    @DisplayName("CancelNotAllowedException -> 409 with custom code")
    void cancelNotAllowed() {
        var b = body(handler.cancelNotAllowed(new CancelNotAllowedException("SHIPPED"), req), HttpStatus.CONFLICT);
        assertThat(b.code()).isEqualTo("ORDER-CANCEL-NOT-ALLOWED");
    }

    @Test
    @DisplayName("CancelRateLimitedException -> 429 with custom code")
    void cancelRateLimited() {
        var b = body(handler.cancelRateLimited(new CancelRateLimitedException(3, 24), req), HttpStatus.TOO_MANY_REQUESTS);
        assertThat(b.code()).isEqualTo("ORDER-CANCEL-RATE-LIMIT");
    }

    @Test
    @DisplayName("ServiceUnavailableException -> 503")
    void serviceUnavailable() {
        var b = body(handler.serviceUnavailable(new ServiceUnavailableException("down"), req), HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(b.code()).isEqualTo(ErrorCode.ORDER_SERVICE_UNAVAILABLE.code());
    }

    @Test
    @DisplayName("ValidationException -> 400 carrying field errors")
    void validation_withFieldErrors() {
        var ex = new ValidationException("invalid", Map.of("phone", "must not be blank"));
        var b = body(handler.validation(ex, req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_BAD_REQUEST.code());
        assertThat(b.fieldErrors()).hasSize(1);
        assertThat(b.fieldErrors().get(0).field()).isEqualTo("phone");
        assertThat(b.fieldErrors().get(0).message()).isEqualTo("must not be blank");
    }

    @Test
    @DisplayName("ConstraintViolationException -> 400")
    void constraintViolation() {
        var ex = new ConstraintViolationException("bad param", java.util.Set.of());
        var b = body(handler.constraintViolation(ex, req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_BAD_REQUEST.code());
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 with Malformed JSON prefix")
    void notReadable() {
        var input = mock(org.springframework.http.HttpInputMessage.class);
        var ex = new HttpMessageNotReadableException("boom", new RuntimeException("unexpected char"), input);
        var b = body(handler.notReadable(ex, req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_BAD_REQUEST.code());
        assertThat(b.message()).startsWith("Malformed JSON:");
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409")
    void dataIntegrity() {
        var ex = new DataIntegrityViolationException("dup");
        var b = body(handler.dataIntegrity(ex, req), HttpStatus.CONFLICT);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_CONFLICT.code());
        assertThat(b.message()).isEqualTo("Constraint violation");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403")
    void accessDenied() {
        var b = body(handler.accessDenied(new AccessDeniedException("nope"), req), HttpStatus.FORBIDDEN);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_FORBIDDEN.code());
        assertThat(b.message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AuthenticationCredentialsNotFoundException -> 401")
    void unauthenticated() {
        var b = body(handler.unauthenticated(new AuthenticationCredentialsNotFoundException("x"), req), HttpStatus.UNAUTHORIZED);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_UNAUTHORIZED.code());
    }

    @Test
    @DisplayName("ApplicationException -> 400 with its own code")
    void application() {
        var ex = new ApplicationException("APP-XYZ", "broke") {};
        var b = body(handler.application(ex, req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo("APP-XYZ");
        assertThat(b.message()).isEqualTo("broke");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400")
    void illegalArg() {
        var b = body(handler.illegalArg(new IllegalArgumentException("bad arg"), req), HttpStatus.BAD_REQUEST);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_BAD_REQUEST.code());
        assertThat(b.message()).isEqualTo("bad arg");
    }

    @Test
    @DisplayName("IllegalStateException -> 422")
    void illegalState() {
        var b = body(handler.illegalState(new IllegalStateException("bad state"), req), HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_UNPROCESSABLE.code());
    }

    @Test
    @DisplayName("Unhandled Exception -> 500 with generic message")
    void unknown() {
        var b = body(handler.unknown(new RuntimeException("secret detail"), req), HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(b.code()).isEqualTo(ErrorCode.APP_INTERNAL.code());
        assertThat(b.message()).isEqualTo("Internal server error");
    }
}
