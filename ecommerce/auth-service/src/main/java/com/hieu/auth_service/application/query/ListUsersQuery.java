package com.hieu.auth_service.application.query;

import com.hieu.auth_service.application.common.Query;
import com.hieu.auth_service.application.dto.PageDTO;
import com.hieu.auth_service.application.dto.UserDTO;

/**
 * Lists users with keyset (cursor) pagination.
 *
 * <p>Clients pass an opaque {@code cursor} produced by a previous response; the first
 * page uses {@code null}. Page order is {@code created_at DESC, id DESC}.
 *
 * @param cursor opaque base64 cursor from the previous page, or {@code null}
 * @param limit  maximum rows to return (clamped by the handler)
 */
public record ListUsersQuery(
        String cursor,
        int limit
) implements Query<PageDTO<UserDTO>> {
}
