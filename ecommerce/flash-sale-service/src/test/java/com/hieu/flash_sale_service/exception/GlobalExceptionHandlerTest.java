package com.hieu.flash_sale_service.exception;

import com.hieu.common.api.ApiResponse;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}. Each handler is invoked directly with its
 * exception and the returned {@link ResponseEntity} is asserted for HTTP status + error envelope.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("FlashSaleNotFoundException -> 404 NOT_FOUND")
    void notFound() {
        var ex = new FlashSaleNotFoundException("7");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isFalse();
        assertThat(resp.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("InsufficientSlotsException -> 409 CONFLICT INSUFFICIENT_SLOTS")
    void insufficient() {
        var ex = new InsufficientSlotsException("3");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleInsufficient(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().code()).isEqualTo("INSUFFICIENT_SLOTS");
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("SaleNotActiveException -> 422 UNPROCESSABLE_ENTITY SALE_NOT_ACTIVE")
    void notActive() {
        var ex = new SaleNotActiveException("5");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotActive(ex);

        assertThat(resp.getStatusCode().value()).isEqualTo(422);
        assertThat(resp.getBody().code()).isEqualTo("SALE_NOT_ACTIVE");
    }

    @Test
    @DisplayName("UserQuotaExceededException -> 409 CONFLICT QUOTA_EXCEEDED")
    void quotaExceeded() {
        var ex = new UserQuotaExceededException("u1", "5", 2);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleQuotaExceeded(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().code()).isEqualTo("QUOTA_EXCEEDED");
        assertThat(resp.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("InvalidStateTransitionException -> 409 CONFLICT INVALID_STATE")
    void invalidTransition() {
        var ex = new InvalidStateTransitionException("5", FlashSaleStatus.ENDED, FlashSaleStatus.ACTIVE);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleInvalidTransition(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().code()).isEqualTo("INVALID_STATE");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 BAD_REQUEST")
    void illegalArg() {
        var ex = new IllegalArgumentException("startTime must be before endTime");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArg(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(resp.getBody().message()).isEqualTo("startTime must be before endTime");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 VALIDATION_ERROR joining field errors")
    void validation() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "createFlashSaleRequest");
        bindingResult.addError(new FieldError("createFlashSaleRequest", "quantity", "must be at least 1"));
        bindingResult.addError(new FieldError("createFlashSaleRequest", "salePrice", "must not be null"));
        var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(resp.getBody().message())
                .contains("quantity: must be at least 1")
                .contains("salePrice: must not be null");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 BAD_REQUEST with 'Malformed JSON'")
    void notReadable() {
        var ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(new RuntimeException("bad token"));

        ResponseEntity<ApiResponse<Void>> resp = handler.notReadable(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(resp.getBody().message()).isEqualTo("Malformed JSON");
    }

    @Test
    @DisplayName("AuthorizationDeniedException -> 403 FORBIDDEN with generic 'Access denied'")
    void authorizationDenied() {
        var ex = new AuthorizationDeniedException("denied");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().code()).isEqualTo("FORBIDDEN");
        assertThat(resp.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 FORBIDDEN")
    void accessDenied() {
        var ex = new AccessDeniedException("nope");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("generic Exception -> 500 INTERNAL_ERROR, message not leaked")
    void generic() {
        var ex = new RuntimeException("boom with internal detail");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(resp.getBody().message()).isEqualTo("Internal server error");
    }
}
