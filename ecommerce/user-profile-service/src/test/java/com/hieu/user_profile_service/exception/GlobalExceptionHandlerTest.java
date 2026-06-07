package com.hieu.user_profile_service.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}: each {@code @ExceptionHandler}
 * method is invoked directly with its exception and the returned {@link ResponseEntity}
 * is asserted for HTTP status + {@link ErrorResponse} code/body. No MockMvc, no Spring context.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static WebRequest request(String desc) {
        WebRequest req = mock(WebRequest.class);
        when(req.getDescription(false)).thenReturn(desc);
        return req;
    }

    @Test
    @DisplayName("UserProfileNotFound -> 404 NOT_FOUND with NOT_FOUND code and exception message")
    void handleNotFound() {
        var ex = new UserProfileNotFoundException("user-42");
        ResponseEntity<ErrorResponse> resp = handler.handleNotFound(ex, request("uri=/api/v1/user-profiles/user-42"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.NOT_FOUND.code());
        assertThat(resp.getBody().message()).isEqualTo("User profile not found: user-42");
        assertThat(resp.getBody().path()).isEqualTo("uri=/api/v1/user-profiles/user-42");
        assertThat(resp.getBody().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("AddressNotFound -> 404 NOT_FOUND with NOT_FOUND code and exception message")
    void handleAddressNotFound() {
        var ex = new AddressNotFoundException("7");
        ResponseEntity<ErrorResponse> resp = handler.handleAddressNotFound(ex, request("uri=/addr"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.NOT_FOUND.code());
        assertThat(resp.getBody().message()).isEqualTo("Address not found: 7");
    }

    @Test
    @DisplayName("MethodArgumentNotValid -> 400 with VALIDATION_FAILED code and mapped field errors")
    void handleValidation() {
        BindingResult br = mock(BindingResult.class);
        var fe1 = new FieldError("upsertAddressRequest", "city", "", false, null, null, "must not be blank");
        var fe2 = new FieldError("upsertAddressRequest", "street", null, false, null, null, "required");
        when(br.getFieldErrors()).thenReturn(List.of(fe1, fe2));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(br);

        ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex, request("uri=/me/addresses"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().fieldErrors()).hasSize(2);
        assertThat(resp.getBody().fieldErrors())
                .extracting(ErrorResponse.FieldError::field)
                .containsExactly("city", "street");
        assertThat(resp.getBody().fieldErrors().get(0).message()).isEqualTo("must not be blank");
        assertThat(resp.getBody().fieldErrors().get(0).rejectedValue()).isEqualTo("");
    }

    @Test
    @DisplayName("HttpMessageNotReadable -> 400 with VALIDATION_FAILED code and 'Malformed JSON' message from the most-specific cause")
    void handleNotReadable_usesMostSpecificCause() {
        var cause = new IllegalArgumentException("Unexpected end-of-input");
        var ex = new HttpMessageNotReadableException("outer", cause, null);

        ResponseEntity<ErrorResponse> resp = handler.handleNotReadable(ex, request("uri=/me"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
        assertThat(resp.getBody().message()).startsWith("Malformed JSON: ");
        assertThat(resp.getBody().message()).contains("Unexpected end-of-input");
    }

    @Test
    @DisplayName("generic Exception -> 500 INTERNAL_ERROR with a fixed safe message (no leak)")
    void handleGeneric() {
        var ex = new RuntimeException("sensitive stack detail");

        ResponseEntity<ErrorResponse> resp = handler.handleGeneric(ex, request("uri=/me"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.code());
        assertThat(resp.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(resp.getBody().message()).doesNotContain("sensitive");
    }
}
