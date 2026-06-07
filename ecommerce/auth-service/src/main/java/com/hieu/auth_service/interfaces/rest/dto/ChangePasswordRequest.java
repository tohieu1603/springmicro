package com.hieu.auth_service.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming payload for {@code POST /api/users/me/password}.
 *
 * <p>Both the old and new passwords are required. The handler verifies the old
 * password inside the {@code User} aggregate before applying the change; any
 * outstanding refresh tokens are revoked and the user's {@code tokenVersion} is
 * bumped so every access token issued earlier becomes invalid.
 */
@Schema(description = "Change-password payload for the current user")
public record ChangePasswordRequest(
        @Schema(description = "Current password")
        @NotBlank String oldPassword,

        @Schema(description = "Desired new password")
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
