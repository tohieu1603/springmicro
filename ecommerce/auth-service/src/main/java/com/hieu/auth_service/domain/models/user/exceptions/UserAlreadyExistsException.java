package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String reason) {
        super(ErrorCode.USER_ALREADY_EXISTS.code(), "User already exists: " + reason);
    }
}
