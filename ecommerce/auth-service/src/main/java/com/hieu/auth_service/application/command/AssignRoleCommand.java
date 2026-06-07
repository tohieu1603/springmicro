package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;

/**
 * Assigns a role to a user (admin-only use case).
 *
 * @param userId   target user id
 * @param roleName canonical role name (e.g. {@code ROLE_ADMIN})
 */
public record AssignRoleCommand(String userId, String roleName) implements Command<Void> {
}
