package com.hieu.auth_service.application.query;

import com.hieu.auth_service.application.common.Query;

/**
 * Checks whether a user effectively has a specific permission through any of their roles.
 *
 * @param userId         target user id
 * @param permissionName canonical permission name (e.g. {@code USER_READ})
 */
public record CheckPermissionQuery(String userId, String permissionName) implements Query<Boolean> {
}
