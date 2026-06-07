package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;

/**
 * Removes a role from a user (admin-only use case).
 *
 * @param userId   target user id
 * @param roleName canonical role name
 */
public record UnassignRoleCommand(String userId, String roleName) implements Command<Void> {
}
