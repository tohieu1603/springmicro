package com.hieu.auth_service.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code POST /api/auth/google}.
 *
 * <p>{@code idToken} is the {@code credential} string Google Identity Services
 * returns to the browser on successful sign-in — a signed JWT, not an OAuth
 * access token. The server verifies its signature, audience and expiry against
 * Google's JWKs before trusting any claim.
 *
 * @param idToken Google-issued ID token (JWT)
 */
@Schema(description = "Body for the Login-with-Google endpoint")
public record GoogleLoginRequest(
        @Schema(description = "Google ID token (`credential` from Google Identity Services)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String idToken
) {
}
