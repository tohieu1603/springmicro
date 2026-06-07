package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

/**
 * Raised when the OAuth provider hands us a profile whose primary email is
 * not yet verified. We refuse to bind a HIEU account to an unverified email
 * because it would let an attacker hijack a future HIEU registration of the
 * same address.
 */
public final class OAuthEmailNotVerifiedException extends DomainException {
    public OAuthEmailNotVerifiedException() {
        super(ErrorCode.OAUTH_EMAIL_UNVERIFIED.code(),
                "OAuth provider email is not verified");
    }
}
