package com.hieu.auth_service.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hieu.auth_service.application.command.ChangeAccountStatusCommand;
import com.hieu.auth_service.application.command.ChangeAccountStatusCommand.Transition;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;

/**
 * Unit tests for {@link ChangeAccountStatusHandler}: the security-critical rule that locking or
 * disabling an account terminates live sessions immediately (tokenVersion bump + refresh-token
 * revocation), while unlock/enable do not.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeAccountStatusHandler (unit)")
class ChangeAccountStatusHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private ChangeAccountStatusHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        handler = new ChangeAccountStatusHandler(userRepository, refreshTokenRepository);
        user = User.register(Username.of("statususer"), Email.of("status@example.com"),
                Password.createRaw("password1"), PersonName.of("Sta", "Tus"), new FakePasswordEncoder());
    }

    private ChangeAccountStatusCommand cmd(Transition transition) {
        return new ChangeAccountStatusCommand(user.getId().value(), transition);
    }

    @Test
    @DisplayName("DISABLE bumps tokenVersion and revokes all refresh tokens")
    void disable_terminatesSessions() {
        int versionBefore = user.getTokenVersion();
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));

        handler.handle(cmd(Transition.DISABLE));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().isActive()).isFalse();
        assertThat(saved.getValue().getTokenVersion()).isEqualTo(versionBefore + 1);
        verify(refreshTokenRepository).revokeAllTokensForUser(user.getId());
    }

    @Test
    @DisplayName("LOCK bumps tokenVersion and revokes all refresh tokens")
    void lock_terminatesSessions() {
        int versionBefore = user.getTokenVersion();
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));

        handler.handle(cmd(Transition.LOCK));

        assertThat(user.getTokenVersion()).isEqualTo(versionBefore + 1);
        verify(refreshTokenRepository).revokeAllTokensForUser(user.getId());
    }

    @Test
    @DisplayName("ENABLE does not bump tokenVersion or revoke tokens")
    void enable_leavesSessionsIntact() {
        int versionBefore = user.getTokenVersion();
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));

        handler.handle(cmd(Transition.ENABLE));

        assertThat(user.getTokenVersion()).isEqualTo(versionBefore);
        verify(refreshTokenRepository, never()).revokeAllTokensForUser(any());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("UNLOCK does not revoke tokens")
    void unlock_leavesSessionsIntact() {
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));

        handler.handle(cmd(Transition.UNLOCK));

        verify(refreshTokenRepository, never()).revokeAllTokensForUser(any());
    }

    @Test
    @DisplayName("unknown user → UserNotFoundException, nothing saved or revoked")
    void unknownUser_throws() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(cmd(Transition.DISABLE)))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllTokensForUser(any());
    }
}
