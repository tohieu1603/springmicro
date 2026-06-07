package com.hieu.auth_service.domain.models.refreshtoken.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class TokenExpiredException extends DomainException {
    private final String tokenId;

    public TokenExpiredException(String tokenId) {
        super(ErrorCode.TOKEN_EXPIRED.code(), "Refresh token has expired");
        this.tokenId = tokenId;
    }

    public String tokenId() { return tokenId; }
}
