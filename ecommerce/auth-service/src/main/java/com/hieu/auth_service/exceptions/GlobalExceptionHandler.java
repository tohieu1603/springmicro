package com.hieu.auth_service.exceptions;

import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenExpiredException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenOwnershipException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenReuseDetectedException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenRevokedException;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
import com.hieu.auth_service.domain.models.user.exceptions.UserAlreadyExistsException;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Central mapping of exceptions to HTTP responses.
 *
 * <p>Domain exceptions funnel through a single handler: HTTP status is derived from the
 * exception type via pattern matching, and the stable {@link DomainException#code()}
 * is surfaced in the response so clients can branch without parsing messages.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Single entry point for every domain-level exception — routing via pattern matching. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex, WebRequest request) {
        HttpStatus status = switch (ex) {
            case UserNotFoundException ignored       -> HttpStatus.NOT_FOUND;
            case UserAlreadyExistsException ignored  -> HttpStatus.CONFLICT;
            case InvalidCredentialsException ignored -> HttpStatus.UNAUTHORIZED;
            case TokenExpiredException ignored       -> HttpStatus.UNAUTHORIZED;
            case TokenRevokedException ignored       -> HttpStatus.UNAUTHORIZED;
            case TokenReuseDetectedException ignored -> HttpStatus.UNAUTHORIZED;
            case TokenOwnershipException ignored     -> HttpStatus.FORBIDDEN;
            case AccountNotUsableException ignored   -> HttpStatus.FORBIDDEN;
            default                                   -> HttpStatus.BAD_REQUEST;
        };
        log.warn("{} [{}]: {}", ex.getClass().getSimpleName(), ex.code(), ex.getMessage());
        return buildResponse(status, ex.code(), ex.getMessage(), request);
    }

    // ── Spring Security / validation / JWT ────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> fields = ex.getBindingResult().getAllErrors().stream()
                .map(e -> ErrorResponse.ValidationError.builder()
                        .field(((FieldError) e).getField())
                        .message(e.getDefaultMessage())
                        .build())
                .toList();

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed")
                .path(pathOf(request))
                .validationErrors(fields)
                .build();
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS.code(), "Invalid username or password", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.code(), "Authentication failed", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.code(),
                "You don't have permission to access this resource", request);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtExpired(ExpiredJwtException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED.code(), "JWT token has expired", request);
    }

    @ExceptionHandler({SignatureException.class, MalformedJwtException.class})
    public ResponseEntity<ErrorResponse> handleJwt(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID.code(), "Invalid JWT token", request);
    }

    /** Malformed JSON body, missing required fields, primitive coercion failures → 400 not 500. */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest request) {
        var cause = ex.getMostSpecificCause();
        var msg = cause != null ? cause.getMessage() : ex.getMessage();
        log.warn("Malformed JSON: {}", msg);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.APP_BAD_REQUEST.code(), "Malformed JSON: " + msg, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.code(),
                "An unexpected error occurred. Please try again later.", request);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String code,
                                                        String message, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(code)
                .message(message)
                .path(pathOf(request))
                .build();
        return new ResponseEntity<>(body, status);
    }

    private static String pathOf(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
