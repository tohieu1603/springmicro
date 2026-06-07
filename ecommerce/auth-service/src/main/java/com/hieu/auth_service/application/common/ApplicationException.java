package com.hieu.auth_service.application.common;

/**
 * Base class for application-layer exceptions.
 *
 * <p>Distinct from {@code DomainException} (which models business rule violations),
 * application exceptions cover orchestration-level failures: missing input,
 * unauthorized use-cases, preconditions specific to a use-case flow.
 *
 * <p>Every exception carries a machine-readable {@link #code()} following the
 * {@code APP-<NNNN>} convention so the web layer can map them to stable HTTP
 * responses without parsing messages.
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

    /**
     * @return stable, machine-readable error code
     */
    public final String code() {
        return code;
    }
}
