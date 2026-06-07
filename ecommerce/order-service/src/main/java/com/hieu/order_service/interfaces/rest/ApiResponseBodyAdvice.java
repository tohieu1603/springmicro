package com.hieu.order_service.interfaces.rest;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.error.ErrorResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Wraps every non-error controller return value in an {@link ApiResponse} envelope so
 * callers can branch on {@code success}/{@code code}/{@code data} uniformly. Error
 * responses already produced by {@link GlobalExceptionHandler} (and anything that's
 * already an {@link ApiResponse} or {@link ErrorResponse}) pass through untouched.
 *
 * <p>Scope is restricted to the controllers in {@code com.hieu.order_service.interfaces.rest}
 * so the advice doesn't accidentally wrap actuator/swagger JSON.
 */
@RestControllerAdvice(basePackages = "com.hieu.order_service.interfaces.rest")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> || body instanceof ErrorResponse) return body;
        // String bodies go through StringHttpMessageConverter — wrapping them as JSON
        // would corrupt the content-type contract (e.g. swagger-ui responses). Pass through.
        if (body instanceof String) return body;
        return ApiResponse.ok(body);
    }
}
