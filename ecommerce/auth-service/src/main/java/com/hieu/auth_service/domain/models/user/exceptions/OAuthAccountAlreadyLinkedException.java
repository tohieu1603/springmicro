package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

/**
 * Raised when the user account already has the given OAuth provider linked.
 * Re-linking the same provider/sub is a no-op upstream; this exception is for
 * the rarer case where a <i>different</i> sub from the same provider is
 * attempted (e.g. user tries to link a second Google account onto a single
 * HIEU account, which we don't support today).
 */
public final class OAuthAccountAlreadyLinkedException extends DomainException {
    public OAuthAccountAlreadyLinkedException(String provider) {
        super(ErrorCode.OAUTH_ACCOUNT_LINKED.code(),
                "Account already linked with another " + provider + " identity");
    }
}
