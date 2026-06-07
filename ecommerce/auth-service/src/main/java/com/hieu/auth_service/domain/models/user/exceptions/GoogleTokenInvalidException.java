package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

/**
 * Raised when a Google ID token fails verification: bad signature, wrong
 * audience, expired, or claims missing required fields. Mapped to HTTP 401
 * by the global handler so callers can treat it like a regular auth failure.
 */
public final class GoogleTokenInvalidException extends DomainException {
    public GoogleTokenInvalidException(String detail) {
        super(ErrorCode.OAUTH_TOKEN_INVALID.code(), "Invalid Google token: " + detail);
    }
}
