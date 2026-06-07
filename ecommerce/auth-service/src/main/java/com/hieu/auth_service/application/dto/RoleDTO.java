package com.hieu.auth_service.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Set;

/**
 * Read model for a role, exposed via queries.
 *
 * @param id          stable role UUID as string
 * @param name        canonical role name (e.g. {@code ROLE_ADMIN})
 * @param description human-readable description, nullable
 * @param permissions permission names granted through this role
 * @param createdAt   creation timestamp
 * @param updatedAt   last-modified timestamp
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoleDTO(
        String id,
        String name,
        String description,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
