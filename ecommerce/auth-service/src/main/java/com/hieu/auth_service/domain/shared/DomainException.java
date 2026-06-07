package com.hieu.auth_service.domain.shared;

/**
 * Root of the domain-exception hierarchy.
 *
 * <p>Every domain error carries a stable {@link #code()} that clients can branch on
 * without parsing human-readable messages. Codes follow the pattern
 * {@code <DOMAIN>-<NNNN>} (e.g. {@code AUTH-1001}) and must never be renamed —
 * only added.
 *
 * <p>Sealed so infrastructure/application layers can use exhaustive switches when
 * mapping domain errors to HTTP/gRPC responses.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public final String code() {
        return code;
    }
}
