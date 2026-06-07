package com.hieu.order_service.application.common;

import com.hieu.common.error.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationException extends ApplicationException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(ErrorCode.APP_BAD_REQUEST.code(), message);
        this.fieldErrors = new LinkedHashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(ErrorCode.APP_BAD_REQUEST.code(), message);
        this.fieldErrors = fieldErrors == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> fieldErrors() { return Map.copyOf(fieldErrors); }
    public boolean hasFieldErrors() { return !fieldErrors.isEmpty(); }
}
