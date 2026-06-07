package com.hieu.auth_service.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenExpiredException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenOwnershipException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenReuseDetectedException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenRevokedException;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
import com.hieu.auth_service.domain.models.user.exceptions.UserAlreadyExistsException;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}: invokes each handler method directly with
 * its exception and asserts the HTTP status + error code/body surfaced in {@link ErrorResponse}.
 * Exercises the domain pattern-match switch (per status) and the Spring/JWT handlers. No MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    WebRequest request;

    @BeforeEach
    void stubPath() {
        when(request.getDescription(false)).thenReturn("uri=/api/v1/auth/login");
    }

    // ── domain switch ─────────────────────────────────────────────────────

    @Test
    void userNotFound_maps404() {
        ResponseEntity<ErrorResponse> r = handler.handleDomain(new UserNotFoundException("bob"), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().getStatus()).isEqualTo(404);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.USER_NOT_FOUND.code());
        assertThat(r.getBody().getPath()).isEqualTo("/api/v1/auth/login");
        assertThat(r.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void userAlreadyExists_maps409() {
        ResponseEntity<ErrorResponse> r = handler.handleDomain(new UserAlreadyExistsException("dup"), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS.code());
    }

    @Test
    void invalidCredentials_maps401() {
        ResponseEntity<ErrorResponse> r = handler.handleDomain(new InvalidCredentialsException(), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.code());
    }

    @Test
    void tokenExpired_maps401() {
        ResponseEntity<ErrorResponse> r = handler.handleDomain(new TokenExpiredException("tid"), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.TOKEN_EXPIRED.code());
    }

    @Test
    void tokenRevoked_maps401() {
        ResponseEntity<ErrorResponse> r =
                handler.handleDomain(new TokenRevokedException("tid", RevokedReason.NORMAL), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.TOKEN_REVOKED.code());
    }

    @Test
    void tokenReuseDetected_maps401() {
        ResponseEntity<ErrorResponse> r =
                handler.handleDomain(new TokenReuseDetectedException("fam", 2), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.TOKEN_REUSE_DETECTED.code());
    }

    @Test
    void tokenOwnership_maps403() {
        ResponseEntity<ErrorResponse> r = handler.handleDomain(new TokenOwnershipException("uid"), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.TOKEN_OWNERSHIP_FAIL.code());
    }

    @Test
    void accountNotUsable_maps403() {
        ResponseEntity<ErrorResponse> r = handler.handleDomain(
                new AccountNotUsableException(AccountNotUsableException.Reason.LOCKED), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.ACCOUNT_LOCKED.code());
    }

    @Test
    void unknownDomainException_fallsBackToBadRequest() {
        DomainException unknown = new DomainException("AUTH-9999", "weird") { };

        ResponseEntity<ErrorResponse> r = handler.handleDomain(unknown, request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().getError()).isEqualTo("AUTH-9999");
        assertThat(r.getBody().getMessage()).isEqualTo("weird");
    }

    // ── validation / security / JWT ─────────────────────────────────────────

    @Test
    void methodArgumentNotValid_returnsFieldErrors() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "loginRequest");
        binding.addError(new FieldError("loginRequest", "username", "must not be blank"));
        // any method works as the MethodParameter source for the exception
        var method = this.getClass().getDeclaredMethod("methodArgumentNotValid_returnsFieldErrors");
        var param = new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, binding);

        ResponseEntity<ErrorResponse> r = handler.handleValidation(ex, request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().getError()).isEqualTo("Validation Failed");
        List<ErrorResponse.ValidationError> fields = r.getBody().getValidationErrors();
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getField()).isEqualTo("username");
        assertThat(fields.get(0).getMessage()).isEqualTo("must not be blank");
    }

    @Test
    void badCredentials_maps401() {
        ResponseEntity<ErrorResponse> r = handler.handleBadCredentials(new BadCredentialsException("nope"), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.code());
    }

    @Test
    void accessDenied_maps403() {
        ResponseEntity<ErrorResponse> r = handler.handleAccessDenied(new AccessDeniedException("no"), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.FORBIDDEN.code());
    }

    @Test
    void jwtExpired_maps401() {
        ExpiredJwtException ex = new ExpiredJwtException(null, null, "expired");
        ResponseEntity<ErrorResponse> r = handler.handleJwtExpired(ex, request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.TOKEN_EXPIRED.code());
    }

    @Test
    void jwtSignatureOrMalformed_maps401Invalid() {
        ResponseEntity<ErrorResponse> sig = handler.handleJwt(new SignatureException("bad sig"), request);
        ResponseEntity<ErrorResponse> mal = handler.handleJwt(new MalformedJwtException("bad"), request);

        assertThat(sig.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(sig.getBody().getError()).isEqualTo(ErrorCode.TOKEN_INVALID.code());
        assertThat(mal.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(mal.getBody().getError()).isEqualTo(ErrorCode.TOKEN_INVALID.code());
    }

    @Test
    void notReadable_maps400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad json", new RuntimeException("Unexpected token"), null);

        ResponseEntity<ErrorResponse> r = handler.handleNotReadable(ex, request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.APP_BAD_REQUEST.code());
        assertThat(r.getBody().getMessage()).contains("Malformed JSON");
    }

    @Test
    void unexpected_maps500() {
        ResponseEntity<ErrorResponse> r = handler.handleAll(new RuntimeException("boom"), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().getError()).isEqualTo(ErrorCode.INTERNAL_ERROR.code());
    }
}
