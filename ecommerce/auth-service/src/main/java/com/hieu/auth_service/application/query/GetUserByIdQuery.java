package com.hieu.auth_service.application.query;

import com.hieu.auth_service.application.common.Query;
import com.hieu.auth_service.application.dto.UserDTO;

/**
 * Retrieves a single user's profile by id.
 *
 * @param userId stable user UUID as string
 */
public record GetUserByIdQuery(String userId) implements Query<UserDTO> {
}
