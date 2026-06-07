package com.hieu.catalog_service.interfaces.rest;

import com.hieu.catalog_service.application.common.ApplicationException;
import com.hieu.catalog_service.application.common.ValidationException;
import com.hieu.catalog_service.domain.exception.AttrAlreadyExistsException;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.exception.InvalidProductStateException;
import com.hieu.catalog_service.domain.exception.ProductAlreadyExistsException;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.exception.VariantNotFoundException;
import com.hieu.common.error.ErrorCode;
import com.hieu.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for the @RestControllerAdvice exception mapping. Each handler is invoked
 * directly with its exception; we assert the HTTP status, stable error code, message and
 * that the request path is echoed into the body. No MockMvc / Spring context.
 */
@DisplayName("GlobalExceptionHandler — unit")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/products/1");
    }

    private static void assertBody(ResponseEntity<ErrorResponse> resp, HttpStatus status, String code) {
        assertThat(resp.getStatusCode()).isEqualTo(status);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(code);
        assertThat(resp.getBody().path()).isEqualTo("/api/v1/products/1");
        assertThat(resp.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("not-found domain exceptions → 404 with the exception's code + message")
    void notFound() {
        var resp = handler.notFound(new ProductNotFoundException("1"), req);

        assertBody(resp, HttpStatus.NOT_FOUND, ErrorCode.PRODUCT_NOT_FOUND.code());
        assertThat(resp.getBody().message()).isEqualTo("Product not found: 1");
    }

    @Test
    @DisplayName("category-not-found routed through the same 404 handler keeps its own code")
    void notFound_category() {
        var resp = handler.notFound(new CategoryNotFoundException("5"), req);

        assertBody(resp, HttpStatus.NOT_FOUND, ErrorCode.CATEGORY_NOT_FOUND.code());
    }

    @Test
    @DisplayName("variant-not-found-by-sku → 404")
    void notFound_variantBySku() {
        var resp = handler.notFound(VariantNotFoundException.bySku("SKU-X"), req);

        assertBody(resp, HttpStatus.NOT_FOUND, ErrorCode.VARIANT_NOT_FOUND.code());
    }

    @Test
    @DisplayName("already-exists domain exceptions → 409 conflict")
    void conflict() {
        var resp = handler.conflict(new ProductAlreadyExistsException("dup-slug"), req);

        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.PRODUCT_ALREADY_EXISTS.code());
    }

    @Test
    @DisplayName("attr-already-exists also → 409")
    void conflict_attr() {
        var resp = handler.conflict(new AttrAlreadyExistsException("COLOR"), req);

        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.ATTR_ALREADY_EXISTS.code());
    }

    @Test
    @DisplayName("invalid product state → 422 unprocessable")
    void invalidState() {
        var resp = handler.invalidState(new InvalidProductStateException("cannot activate"), req);

        assertBody(resp, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.PRODUCT_INVALID_STATE.code());
        assertThat(resp.getBody().message()).isEqualTo("cannot activate");
    }

    @Test
    @DisplayName("ValidationException → 400 with field errors projected into the body")
    void validation() {
        var ex = new ValidationException("bad input", Map.of("name", "must not be blank"));

        var resp = handler.validation(ex, req);

        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().fieldErrors()).singleElement().satisfies(fe -> {
            assertThat(fe.field()).isEqualTo("name");
            assertThat(fe.message()).isEqualTo("must not be blank");
            assertThat(fe.rejectedValue()).isNull();
        });
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with 'Malformed JSON' prefix")
    void notReadable() {
        var ex = new HttpMessageNotReadableException("boom", (org.springframework.http.HttpInputMessage) null);

        var resp = handler.notReadable(ex, req);

        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).startsWith("Malformed JSON: ");
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 with generic message")
    void dataIntegrity() {
        var ex = new DataIntegrityViolationException("duplicate key");

        var resp = handler.dataIntegrity(ex, req);

        assertBody(resp, HttpStatus.CONFLICT, ErrorCode.APP_CONFLICT.code());
        assertThat(resp.getBody().message()).isEqualTo("Constraint violation");
    }

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied() {
        var resp = handler.accessDenied(new AccessDeniedException("nope"), req);

        assertBody(resp, HttpStatus.FORBIDDEN, ErrorCode.APP_FORBIDDEN.code());
        assertThat(resp.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AuthenticationCredentialsNotFoundException → 401")
    void unauthenticated() {
        var resp = handler.unauthenticated(new AuthenticationCredentialsNotFoundException("none"), req);

        assertBody(resp, HttpStatus.UNAUTHORIZED, ErrorCode.APP_UNAUTHORIZED.code());
        assertThat(resp.getBody().message()).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("ApplicationException → 400 with the exception's code")
    void application() {
        ApplicationException ex = new ApplicationException("APP-400", "boom") {};

        var resp = handler.application(ex, req);

        assertBody(resp, HttpStatus.BAD_REQUEST, "APP-400");
        assertThat(resp.getBody().message()).isEqualTo("boom");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 echoing the message")
    void illegalArg() {
        var resp = handler.illegalArg(new IllegalArgumentException("bad arg"), req);

        assertBody(resp, HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code());
        assertThat(resp.getBody().message()).isEqualTo("bad arg");
    }

    @Test
    @DisplayName("IllegalStateException → 422 echoing the message")
    void illegalState() {
        var resp = handler.illegalState(new IllegalStateException("bad state"), req);

        assertBody(resp, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.APP_UNPROCESSABLE.code());
        assertThat(resp.getBody().message()).isEqualTo("bad state");
    }

    @Test
    @DisplayName("unhandled Exception → 500 with a sanitised generic message")
    void unknown() {
        var resp = handler.unknown(new RuntimeException("leak me"), req);

        assertBody(resp, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.APP_INTERNAL.code());
        assertThat(resp.getBody().message()).isEqualTo("Internal server error");
    }
}
