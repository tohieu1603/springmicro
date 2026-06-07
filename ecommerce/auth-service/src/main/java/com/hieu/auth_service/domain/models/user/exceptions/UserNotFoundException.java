package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class UserNotFoundException extends DomainException {
    public UserNotFoundException(String lookup) {
        super(ErrorCode.USER_NOT_FOUND.code(), "User not found: " + lookup);
    }
}
