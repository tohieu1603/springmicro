package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;

/**
 * Changes the current user's password.
 *
 * <p>Side effects enforced by the handler:
 * <ul>
 *   <li>Verifies the old password.</li>
 *   <li>Bumps {@code tokenVersion}, invalidating every existing access token.</li>
 *   <li>Revokes all outstanding refresh tokens for the user.</li>
 *   <li>C3: Blacklists the caller's current access token JTI immediately.</li>
 * </ul>
 *
 * @param userId                 subject user id (from authenticated context)
 * @param oldRawPassword         user's current password
 * @param newRawPassword         desired new password
 * @param currentAccessTokenJti  jti of the caller's current access token; may be null (BC/non-JWT clients)
 * @param currentAccessTokenExp  expiry of the caller's current access token; may be null
 */
public record ChangePasswordCommand(
        String userId,
        String oldRawPassword,
        String newRawPassword,
        String currentAccessTokenJti,
        java.time.Instant currentAccessTokenExp
) implements Command<Void> {
}
