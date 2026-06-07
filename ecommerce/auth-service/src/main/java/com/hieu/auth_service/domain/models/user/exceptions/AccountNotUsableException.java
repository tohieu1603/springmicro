package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

/**
 * Thrown when an otherwise authenticated user cannot log in due to account-state flags.
 * A single exception with a {@link Reason} beats four near-identical classes — the reason
 * carries the distinct error code so clients can still branch per-case.
 */
public final class AccountNotUsableException extends DomainException {

    public enum Reason {
        DISABLED(ErrorCode.ACCOUNT_DISABLED.code(), "Account is disabled"),
        LOCKED(ErrorCode.ACCOUNT_LOCKED.code(), "Account is locked"),
        EXPIRED(ErrorCode.ACCOUNT_EXPIRED.code(), "Account has expired"),
        CREDENTIALS_EXPIRED(ErrorCode.CREDENTIALS_EXPIRED.code(), "Credentials have expired");

        private final String code;
        private final String message;

        Reason(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String code()    { return code; }
        public String message() { return message; }
    }

    private final Reason reason;

    public AccountNotUsableException(Reason reason) {
        super(reason.code(), reason.message());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
