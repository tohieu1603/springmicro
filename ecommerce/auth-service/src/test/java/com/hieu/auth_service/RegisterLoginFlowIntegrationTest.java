package com.hieu.auth_service;

import com.hieu.auth_service.application.command.LoginCommand;
import com.hieu.auth_service.application.command.RegisterUserCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end happy path: register → login → tokens round-trip.
 *
 * <p>Exercises the full handler chain against real Postgres / Redis / Kafka, proving
 * that:
 * <ul>
 *   <li>BCrypt hashing + verification works outside unit tests</li>
 *   <li>Flyway schema matches the JPA mappings (Hibernate {@code validate} mode)</li>
 *   <li>Refresh tokens persist correctly through the aggregate → mapper → JPA path</li>
 * </ul>
 */
class RegisterLoginFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired CommandHandler<RegisterUserCommand, AuthResponseDTO> registerHandler;
    @Autowired CommandHandler<LoginCommand,        AuthResponseDTO> loginHandler;

    @Test
    void registerThenLogin_returnsDifferentAccessTokensForEachCall() {
        AuthResponseDTO registered = registerHandler.handle(new RegisterUserCommand(
                "alice", "alice@example.com", "P@ssw0rd123", "Alice", "Anderson"));

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.refreshToken()).isNotBlank();
        assertThat(registered.user().username()).isEqualTo("alice");

        AuthResponseDTO loggedIn = loginHandler.handle(new LoginCommand("alice", "P@ssw0rd123"));

        // Two different JWTs must be minted — jti + iat differ even though the user is identical.
        assertThat(loggedIn.accessToken()).isNotEqualTo(registered.accessToken());
        assertThat(loggedIn.user().id()).isEqualTo(registered.user().id());
    }
}
