package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;
import com.hieu.auth_service.application.dto.AuthResponseDTO;

/**
 * Username/email + password authentication.
 *
 * <p>The handler accepts either a username or email in {@code usernameOrEmail}; detection
 * is purely heuristic ("contains @" → email). Wrong-credential and account-state errors
 * surface as distinct domain exceptions so the web layer can map to different HTTP codes.
 *
 * @param usernameOrEmail login identifier
 * @param rawPassword     raw password supplied by the user
 */
public record LoginCommand(
        String usernameOrEmail,
        String rawPassword
) implements Command<AuthResponseDTO> {
}
