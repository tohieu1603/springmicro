package com.hieu.auth_service.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code PATCH /api/users/me/email}.
 */
@Schema(description = "Email-update payload for the current user")
public record UpdateEmailRequest(
        @Schema(example = "new.email@example.com")
        @NotBlank @Email String newEmail
) {
}
