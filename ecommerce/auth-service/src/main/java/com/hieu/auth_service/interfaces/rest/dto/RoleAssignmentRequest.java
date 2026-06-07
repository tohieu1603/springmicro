package com.hieu.auth_service.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin payload for assigning / unassigning a role.
 */
@Schema(description = "Role assignment payload")
public record RoleAssignmentRequest(
        @Schema(example = "ROLE_ADMIN")
        @NotBlank String roleName
) {
}
