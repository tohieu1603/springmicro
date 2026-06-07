package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.ChangePasswordCommand;
import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ChangePasswordHandler} — old-password verification (delegated to the
 * aggregate), bulk refresh-token revocation, and access-token blacklisting (C3). All mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordHandler (unit)")
class ChangePasswordHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistPort tokenBlacklist;

    private final PasswordEncoderPort encoder = new FakePasswordEncoder();

    private ChangePasswordHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        handler = new ChangePasswordHandler(userRepository, refreshTokenRepository, encoder, tokenBlacklist);
        user = User.register(Username.of("alice"), Email.of("alice@example.com"),
                Password.createRaw("password1"), PersonName.of("Alice", "Nguyen"), encoder);
    }

    private ChangePasswordCommand cmd(String jti, Instant exp) {
        return new ChangePasswordCommand(user.getId().value(), "password1", "newpass123", jti, exp);
    }

    @Test
    @DisplayName("happy path changes password, saves user, revokes all refresh tokens, blacklists access token")
    void changePassword_happyPath() {
        Instant exp = Instant.now().plusSeconds(600);
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));

        handler.handle(cmd("jti-123", exp));

        assertThat(user.getPassword().value()).isEqualTo("ENC(newpass123)");
        assertThat(user.getTokenVersion()).isEqualTo(2); // bumped by aggregate
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllTokensForUser(user.getId());
        verify(tokenBlacklist).revoke("jti-123", user.getId().value(), exp, "PASSWORD_CHANGED");
    }

    @Test
    @DisplayName("missing jti/exp skips the access-token blacklist but still revokes refresh tokens")
    void changePassword_noJtiSkipsBlacklist() {
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));

        handler.handle(cmd(null, null));

        verify(refreshTokenRepository).revokeAllTokensForUser(user.getId());
        verifyNoInteractions(tokenBlacklist);
    }

    @Test
    @DisplayName("unknown user → UserNotFoundException, nothing revoked or blacklisted")
    void changePassword_userNotFound() {
        UserId someId = UserId.generate();
        ChangePasswordCommand command = new ChangePasswordCommand(
                someId.value(), "password1", "newpass123", "jti", Instant.now().plusSeconds(60));
        when(userRepository.findById(UserId.of(someId.value()))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllTokensForUser(any());
        verifyNoInteractions(tokenBlacklist);
    }

    @Test
    @DisplayName("wrong old password → InvalidCredentialsException, no revoke/blacklist side effects")
    void changePassword_wrongOldPassword() {
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        ChangePasswordCommand command = new ChangePasswordCommand(
                user.getId().value(), "WRONGpass1", "newpass123", "jti", Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllTokensForUser(any());
        verify(tokenBlacklist, never()).revoke(anyString(), anyString(), any(), eq("PASSWORD_CHANGED"));
    }
}
