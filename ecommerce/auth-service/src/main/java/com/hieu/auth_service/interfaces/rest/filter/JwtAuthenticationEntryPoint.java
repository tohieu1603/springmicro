package com.hieu.auth_service.interfaces.rest.filter;

import tools.jackson.databind.ObjectMapper;
import com.hieu.auth_service.exceptions.ErrorResponse;
import com.hieu.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles Spring Security's {@code 401 Unauthorized} for unauthenticated requests to
 * protected endpoints.
 *
 * <p>Returns a {@link ErrorResponse} JSON payload instead of the default HTML so the
 * response shape matches {@code GlobalExceptionHandler} — clients only need to understand
 * one error schema.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(ErrorCode.UNAUTHORIZED.code())
                .message(authException.getMessage() == null ? "Authentication required" : authException.getMessage())
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
