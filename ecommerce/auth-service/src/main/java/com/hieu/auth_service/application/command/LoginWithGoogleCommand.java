package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;
import com.hieu.auth_service.application.dto.AuthResponseDTO;

/**
 * Logs the caller in using a Google ID token issued by Google Identity Services
 * on the frontend. The handler verifies the token against the configured
 * client ID, then find-or-creates the local user and issues a HIEU JWT pair.
 *
 * <p>The {@code idToken} field carries the {@code credential} payload returned
 * to the FE by Google's One Tap / button widget — a signed JWT, not an OAuth
 * access token. We never see the user's Google password.
 *
 * @param idToken raw Google ID token (JWT) string
 */
public record LoginWithGoogleCommand(String idToken) implements Command<AuthResponseDTO> {
}
