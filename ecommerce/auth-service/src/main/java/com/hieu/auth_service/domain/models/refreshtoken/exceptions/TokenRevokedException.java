package com.hieu.auth_service.domain.models.refreshtoken.exceptions;

import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class TokenRevokedException extends DomainException {
    private final String tokenId;
    // RevokedReason không serializable — đánh transient để class hợp lệ Serializable
    // (DomainException kế thừa RuntimeException implements Serializable).
    private final transient RevokedReason reason;

    public TokenRevokedException(String tokenId, RevokedReason reason) {
        super(ErrorCode.TOKEN_REVOKED.code(),
                "Refresh token has been revoked (" + (reason != null ? reason.value() : "UNKNOWN") + ")");
        this.tokenId = tokenId;
        this.reason = reason;
    }

    public String tokenId()       { return tokenId; }
    public RevokedReason reason() { return reason; }
}
