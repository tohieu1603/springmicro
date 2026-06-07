package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;
import com.hieu.auth_service.application.dto.UserDTO;

/**
 * Changes the current user's email address.
 *
 * @param userId   subject user id (from authenticated context)
 * @param newEmail desired new email address
 */
public record UpdateEmailCommand(String userId, String newEmail) implements Command<UserDTO> {
}
