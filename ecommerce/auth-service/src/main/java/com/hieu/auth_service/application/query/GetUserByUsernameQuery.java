package com.hieu.auth_service.application.query;

import com.hieu.auth_service.application.common.Query;
import com.hieu.auth_service.application.dto.UserDTO;

/**
 * Retrieves a single user's profile by username.
 *
 * @param username unique username
 */
public record GetUserByUsernameQuery(String username) implements Query<UserDTO> {
}
