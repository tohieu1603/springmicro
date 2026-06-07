package com.hieu.auth_service.application.dto;

/**
 * Payload returned after a successful login / register / refresh.
 *
 * @param accessToken       opaque access token (bearer)
 * @param refreshToken      opaque refresh token (bearer)
 * @param tokenType         always {@code "Bearer"} for this service
 * @param expiresInSeconds  remaining lifetime of the access token in seconds
 * @param user              user profile snapshot at the time of issuance
 */
public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserDTO user
) {
    /**
     * Convenience builder that always sets {@code tokenType = "Bearer"}.
     *
     * @param accessToken      access token string
     * @param refreshToken     refresh token string
     * @param expiresInSeconds access-token remaining lifetime
     * @param user             user snapshot
     * @return populated {@code AuthResponseDTO}
     */
    public static AuthResponseDTO bearer(String accessToken, String refreshToken,
                                         long expiresInSeconds, UserDTO user) {
        return new AuthResponseDTO(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
