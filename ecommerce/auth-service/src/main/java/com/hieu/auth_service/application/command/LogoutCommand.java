package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;

/**
 * Revokes the supplied refresh token and blacklists the current access token.
 *
 * <p>Both tokens are required so the session can be terminated cleanly on every client —
 * the access token would otherwise remain valid until natural expiry.
 *
 * @param accessToken  current access token (jti will be blacklisted)
 * @param refreshToken opaque refresh token to revoke
 */
public record LogoutCommand(String accessToken, String refreshToken) implements Command<Void> {
}
