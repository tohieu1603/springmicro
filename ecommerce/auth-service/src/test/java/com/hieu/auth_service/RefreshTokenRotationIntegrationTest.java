package com.hieu.auth_service;

import com.hieu.auth_service.application.command.LoginCommand;
import com.hieu.auth_service.application.command.RefreshTokenCommand;
import com.hieu.auth_service.application.command.RegisterUserCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refresh-token rotation happy path.
 *
 * <p>Verifies that a refresh call:
 * <ol>
 *   <li>Issues a new access + refresh pair (different token strings).</li>
 *   <li>Marks the presented refresh token as revoked in DB.</li>
 *   <li>Keeps the rotated token in the same {@code family} with {@code generation+1}.</li>
 * </ol>
 */
class RefreshTokenRotationIntegrationTest extends AbstractIntegrationTest {

    @Autowired CommandHandler<RegisterUserCommand, AuthResponseDTO> registerHandler;
    @Autowired CommandHandler<LoginCommand,        AuthResponseDTO> loginHandler;
    @Autowired CommandHandler<RefreshTokenCommand, AuthResponseDTO> refreshHandler;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @Test
    void refresh_rotatesTokenWithinSameFamily_andRevokesOldToken() {
        AuthResponseDTO registered = registerHandler.handle(new RegisterUserCommand(
                "rotuser", "rot@example.com", "P@ssw0rd123", "Rot", "User"));
        String oldRefresh = registered.refreshToken();

        AuthResponseDTO rotated = refreshHandler.handle(new RefreshTokenCommand(oldRefresh));

        // New token pair issued — both strings differ from the originals
        assertThat(rotated.accessToken()).isNotEqualTo(registered.accessToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(oldRefresh);

        RefreshToken oldStored = refreshTokenRepository.findByTokenValue(TokenValue.of(oldRefresh))
                .orElseThrow();
        assertThat(oldStored.isRevoked()).isTrue();

        RefreshToken newStored = refreshTokenRepository.findByTokenValue(TokenValue.of(rotated.refreshToken()))
                .orElseThrow();
        assertThat(newStored.getFamily().value()).isEqualTo(oldStored.getFamily().value());
        assertThat(newStored.getGeneration().value()).isEqualTo(oldStored.getGeneration().value() + 1);
        assertThat(newStored.isRevoked()).isFalse();
    }

    @Test
    void login_issuesNewFamilyStartingAtGenerationZero() {
        registerHandler.handle(new RegisterUserCommand(
                "famuser", "fam@example.com", "P@ssw0rd123", "Fam", "User"));

        AuthResponseDTO loggedIn = loginHandler.handle(new LoginCommand("famuser", "P@ssw0rd123"));

        RefreshToken token = refreshTokenRepository.findByTokenValue(TokenValue.of(loggedIn.refreshToken()))
                .orElseThrow();
        // Fresh login = new session = new family starting at generation 0.
        assertThat(token.getGeneration().isRoot()).isTrue();
    }
}
