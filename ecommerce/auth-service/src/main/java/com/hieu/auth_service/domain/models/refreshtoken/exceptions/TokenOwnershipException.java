package com.hieu.auth_service.domain.models.refreshtoken.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class TokenOwnershipException extends DomainException {
    public TokenOwnershipException(String userId) {
        super(ErrorCode.TOKEN_OWNERSHIP_FAIL.code(), "Token does not belong to user: " + userId);
    }
}
