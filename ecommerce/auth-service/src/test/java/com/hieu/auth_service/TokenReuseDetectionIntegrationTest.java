package com.hieu.auth_service;

import com.hieu.auth_service.application.command.RefreshTokenCommand;
import com.hieu.auth_service.application.command.RegisterUserCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenReuseDetectedException;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the refresh-token theft scenario:
 *
 * <ol>
 *   <li>A user rotates their token normally ({@code old} → {@code new}). The old token
 *       is revoked with reason {@code NORMAL}.</li>
 *   <li>An attacker replays the revoked old token. The domain service detects the reuse
 *       attempt and <em>revokes every token in the family</em> (reason {@code FAMILY_REVOKED}),
 *       forcing the legitimate user back to login.</li>
 * </ol>
 */
class TokenReuseDetectionIntegrationTest extends AbstractIntegrationTest {

    @Autowired CommandHandler<RegisterUserCommand, AuthResponseDTO> registerHandler;
    @Autowired CommandHandler<RefreshTokenCommand, AuthResponseDTO> refreshHandler;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @Test
    void reusingRevokedToken_revokesEntireFamily() {
        AuthResponseDTO registered = registerHandler.handle(new RegisterUserCommand(
                "reuse", "reuse@example.com", "P@ssw0rd123", "Re", "Use"));
        String originalRefresh = registered.refreshToken();

        // Legitimate rotation #1
        AuthResponseDTO rotatedOnce = refreshHandler.handle(new RefreshTokenCommand(originalRefresh));
        // Legitimate rotation #2 (family now has three generations)
        refreshHandler.handle(new RefreshTokenCommand(rotatedOnce.refreshToken()));

        // Attacker replays the original (already revoked) token.
        assertThatThrownBy(() -> refreshHandler.handle(new RefreshTokenCommand(originalRefresh)))
                .isInstanceOf(TokenReuseDetectedException.class);

        // Every token in the family must now be revoked.
        TokenFamily family = refreshTokenRepository
                .findByTokenValue(TokenValue.of(originalRefresh))
                .orElseThrow()
                .getFamily();

        List<RefreshToken> familyTokens = refreshTokenRepository.findByFamily(family);
        assertThat(familyTokens).isNotEmpty();
        assertThat(familyTokens).allMatch(RefreshToken::isRevoked);
        // At least one token must carry the security-flavoured revocation reason.
        assertThat(familyTokens).anyMatch(RefreshToken::wasRevokedForSecurity);
    }
}
