package com.hieu.auth_service.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming payload for {@code POST /api/auth/register}.
 *
 * <p>Bean Validation happens at the controller boundary; the domain aggregates
 * perform a second round of invariant checks (so malformed values never reach the DB
 * even if callers bypass validation).
 */
@Schema(description = "New-user registration payload")
public record RegisterRequest(
        @Schema(example = "johndoe", minLength = 3, maxLength = 32)
        @NotBlank @Size(min = 3, max = 32) String username,

        @Schema(example = "john@example.com")
        @NotBlank @Email String email,

        @Schema(description = "Raw password, min 8 chars, must contain letter+digit", example = "P@ssw0rd123")
        @NotBlank @Size(min = 8, max = 100) String password,

        @Schema(example = "John") @NotBlank @Size(max = 64) String firstName,
        @Schema(example = "Doe")  @NotBlank @Size(max = 64) String lastName
) {
}
