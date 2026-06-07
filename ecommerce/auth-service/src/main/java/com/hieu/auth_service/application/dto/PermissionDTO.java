package com.hieu.auth_service.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Read model for a permission.
 *
 * @param id          stable permission UUID as string
 * @param name        canonical permission name (e.g. {@code USER_READ})
 * @param resource    resource segment of the name
 * @param action      action segment of the name
 * @param description human-readable description, nullable
 * @param createdAt   creation timestamp
 * @param updatedAt   last-modified timestamp
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PermissionDTO(
        String id,
        String name,
        String resource,
        String action,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
