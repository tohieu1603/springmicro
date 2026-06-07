package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;
import com.hieu.auth_service.application.dto.AuthResponseDTO;

/**
 * Exchanges a valid refresh token for a new access+refresh pair (rotation).
 *
 * <p>Reuse of an already-revoked token triggers family-wide revocation — this is the
 * standard defense against token theft.
 *
 * @param refreshToken opaque refresh token value previously issued to the client
 */
public record RefreshTokenCommand(String refreshToken) implements Command<AuthResponseDTO> {
}
