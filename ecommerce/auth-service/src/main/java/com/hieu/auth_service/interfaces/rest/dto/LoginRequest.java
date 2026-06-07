package com.hieu.auth_service.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code POST /api/auth/login}.
 *
 * <p>{@code usernameOrEmail} accepts either form — the handler routes by "@" presence.
 */
@Schema(description = "Credentials-based login payload")
public record LoginRequest(
        @Schema(description = "Username or email address", example = "johndoe")
        @NotBlank String usernameOrEmail,

        @Schema(description = "Raw password")
        @NotBlank String password
) {
}
