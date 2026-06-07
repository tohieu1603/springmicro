package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS.code(), "Invalid username or password");
    }
}
