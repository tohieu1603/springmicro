package com.hieu.auth_service.application.common;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hieu.common.error.ErrorCode;

/**
 * Raised when command/query input fails application-level validation.
 *
 * <p>Carries a map of field→error-message to support structured error responses.
 * Field order is preserved via {@link LinkedHashMap} so clients can display
 * errors in the same order they were added.
 */
public final class ValidationException extends ApplicationException {

    private final Map<String, String> fieldErrors;

    /**
     * Creates a validation exception with only a top-level message.
     *
     * @param message human-readable summary
     */
    public ValidationException(String message) {
        super(ErrorCode.APP_BAD_REQUEST.code(), message);
        this.fieldErrors = new LinkedHashMap<>();
    }

    /**
     * Creates a validation exception carrying field-level errors.
     *
     * @param message     top-level summary
     * @param fieldErrors map of {@code field → error message}; a defensive copy is stored
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(ErrorCode.APP_BAD_REQUEST.code(), message);
        this.fieldErrors = fieldErrors == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fieldErrors);
    }

    /** @return unmodifiable view of collected field errors */
    public Map<String, String> fieldErrors() {
        return Map.copyOf(fieldErrors);
    }

    /** @return {@code true} if at least one field error was reported */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
