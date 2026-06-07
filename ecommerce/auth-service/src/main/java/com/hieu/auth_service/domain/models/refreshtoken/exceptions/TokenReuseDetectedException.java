package com.hieu.auth_service.domain.models.refreshtoken.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

/**
 * Raised when a previously-revoked refresh token is re-presented. Treated as token theft:
 * upstream policy is to revoke the entire family and force re-login.
 */
public final class TokenReuseDetectedException extends DomainException {
    private final String family;
    private final int generation;

    public TokenReuseDetectedException(String family, int generation) {
        super(ErrorCode.TOKEN_REUSE_DETECTED.code(),
                "Token reuse detected for family=" + family + " at generation=" + generation
                        + "; all tokens in the family have been revoked.");
        this.family = family;
        this.generation = generation;
    }

    public String family()  { return family; }
    public int generation() { return generation; }
}
