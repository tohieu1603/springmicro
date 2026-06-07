package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;
import com.hieu.auth_service.application.dto.AuthResponseDTO;

/**
 * Registers a brand-new user account and returns an authenticated session.
 *
 * <p>Input is validated by {@link com.hieu.auth_service.domain.models.user.vo.Username},
 * {@link com.hieu.auth_service.domain.models.user.vo.Email}, and
 * {@link com.hieu.auth_service.domain.models.user.vo.Password} value objects. The
 * handler enforces uniqueness via {@code UserRepository}.
 *
 * @param username    desired unique username
 * @param email       unique email address
 * @param rawPassword raw password — hashed inside the aggregate, never persisted raw
 * @param firstName   given name
 * @param lastName    family name
 */
public record RegisterUserCommand(
        String username,
        String email,
        String rawPassword,
        String firstName,
        String lastName
) implements Command<AuthResponseDTO> {
}
