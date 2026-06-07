package com.hieu.payment_service.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}. Each handler method is called
 * directly with its exception and a stubbed request; we assert the HTTP status and the
 * {@link ErrorResponse} body (code + message + path). No Spring / MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/payments/42";

    @Mock HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void stubPath() {
        lenient().when(request.getRequestURI()).thenReturn(PATH);
    }

    @Test
    @DisplayName("PaymentNotFoundException -> 404 NOT_FOUND")
    void notFound() {
        var ex = new PaymentNotFoundException("42");

        ResponseEntity<ErrorResponse> resp = handler.handleNotFound(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.NOT_FOUND.code());
        assertThat(resp.getBody().message()).isEqualTo("Payment not found: 42");
        assertThat(resp.getBody().path()).isEqualTo(PATH);
    }

    @Test
    @DisplayName("InvalidPaymentStateException -> 422 UNPROCESSABLE_ENTITY with CONFLICT code")
    void invalidState() {
        var ex = new InvalidPaymentStateException("cannot confirm a CANCELLED payment");

        ResponseEntity<ErrorResponse> resp = handler.handleInvalidState(ex, request);

        assertThat(resp.getStatusCode().value()).isEqualTo(422);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.CONFLICT.code());
        assertThat(resp.getBody().message()).isEqualTo("cannot confirm a CANCELLED payment");
        assertThat(resp.getBody().path()).isEqualTo(PATH);
    }

    @Test
    @DisplayName("DuplicatePaymentException -> 409 CONFLICT")
    void duplicate() {
        var ex = new DuplicatePaymentException("ORD-1");

        ResponseEntity<ErrorResponse> resp = handler.handleDuplicate(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.CONFLICT.code());
        assertThat(resp.getBody().message()).isEqualTo("Payment already exists for order: ORD-1");
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException -> 409 with retry message")
    void optimisticLock() {
        var ex = new ObjectOptimisticLockingFailureException("Payment", 1L);

        ResponseEntity<ErrorResponse> resp = handler.handleOptimisticLock(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.CONFLICT.code());
        assertThat(resp.getBody().message()).isEqualTo("Concurrent update, please retry");
        assertThat(resp.getBody().path()).isEqualTo(PATH);
    }

    @Test
    @DisplayName("PaymentAccessDeniedException -> 403 FORBIDDEN")
    void accessDenied() {
        var ex = new PaymentAccessDeniedException("7");

        ResponseEntity<ErrorResponse> resp = handler.handleAccessDenied(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.FORBIDDEN.code());
        assertThat(resp.getBody().message()).isEqualTo("Access denied for payment: 7");
    }

    @Test
    @DisplayName("Spring AccessDeniedException -> 403 FORBIDDEN")
    void springAccessDenied() {
        var ex = new org.springframework.security.access.AccessDeniedException("denied");

        ResponseEntity<ErrorResponse> resp = handler.handleSpringAccessDenied(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.FORBIDDEN.code());
        assertThat(resp.getBody().message()).isEqualTo("denied");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 VALIDATION_FAILED")
    void badArg() {
        var ex = new IllegalArgumentException("bad amount");

        ResponseEntity<ErrorResponse> resp = handler.handleBadArg(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().message()).isEqualTo("bad amount");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 with field errors mapped")
    void validation() throws Exception {
        var target = new Object();
        var binding = new BeanPropertyBindingResult(target, "initiatePaymentRequest");
        binding.addError(new FieldError("initiatePaymentRequest", "amount", -5,
                false, null, null, "must be positive"));
        // Any real method works as the MethodParameter source; pick one from this class.
        Method m = GlobalExceptionHandlerTest.class.getDeclaredMethod("validation");
        var ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(m, -1), binding);

        ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().fieldErrors()).singleElement()
                .satisfies(fe -> {
                    assertThat(fe.field()).isEqualTo("amount");
                    assertThat(fe.message()).isEqualTo("must be positive");
                    assertThat(fe.rejectedValue()).isEqualTo(-5);
                });
        assertThat(resp.getBody().path()).isEqualTo(PATH);
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 with most-specific cause in message")
    void notReadable() {
        var cause = new IllegalStateException("Unexpected token");
        var ex = new org.springframework.http.converter.HttpMessageNotReadableException(
                "JSON parse error", cause,
                new org.springframework.http.server.ServletServerHttpRequest(request));

        ResponseEntity<ErrorResponse> resp = handler.handleNotReadable(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().message()).contains("Malformed JSON").contains("Unexpected token");
    }

    @Test
    @DisplayName("generic Exception -> 500 INTERNAL_ERROR with masked message")
    void generic() {
        var ex = new RuntimeException("npe deep in the stack");

        ResponseEntity<ErrorResponse> resp = handler.handleGeneric(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.code());
        // internal detail must NOT leak to the client
        assertThat(resp.getBody().message()).isEqualTo("Internal server error");
    }
}
