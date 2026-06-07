package com.hieu.catalog_service.domain.shared;

/**
 * Root of the domain-exception hierarchy — every concrete domain exception carries
 * a stable {@code code()} so clients can branch without parsing human-readable messages.
 *
 * <p>Codes follow {@code <DOMAIN>-<NNNN>} convention (e.g. {@code CATALOG-2001}) and
 * must never be renamed — only added.
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

    public final String code() { return code; }
}
