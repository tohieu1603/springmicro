package com.hieu.auth_service.application.query;

import com.hieu.auth_service.application.common.Query;

/**
 * Checks whether a user has a specific role assigned.
 *
 * @param userId   target user id
 * @param roleName canonical role name (case-sensitive, e.g. {@code ROLE_ADMIN})
 */
public record CheckRoleQuery(String userId, String roleName) implements Query<Boolean> {
}
