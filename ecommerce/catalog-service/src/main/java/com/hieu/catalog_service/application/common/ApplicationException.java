package com.hieu.catalog_service.application.common;

/**
 * Base class for application-layer exceptions. Distinct from {@code DomainException} —
 * these model orchestration-level failures (invalid input, unauthorized use-case,
 * preconditions specific to a flow). Each carries a machine-readable {@link #code()}
 * for the web layer to map to stable HTTP responses.
 */
public abstract class ApplicationException extends RuntimeException {

    private final String code;

    protected ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected ApplicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public final String code() { return code; }
}
