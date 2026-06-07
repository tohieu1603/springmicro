package com.hieu.order_service.domain.shared;

/** Root of the domain-exception hierarchy. Each concrete exception carries a stable {@code code()}. */
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
