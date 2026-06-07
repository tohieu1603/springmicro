package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.ChangePasswordCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link ChangePasswordCommand}.
 *
 * <p>Delegates credential validation + token-version bump to the {@code User} aggregate,
 * then bulk-revokes all refresh tokens so every outstanding session is invalidated.
 * C3: Also blacklists the caller's current access token JTI to close the race window.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePasswordHandler implements CommandHandler<ChangePasswordCommand, Void> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenBlacklistPort tokenBlacklist;

    @Override
    @Transactional
    public Void handle(ChangePasswordCommand command) {
        User user = userRepository.findById(UserId.of(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        user.changePassword(
                Password.createRaw(command.oldRawPassword()),
                Password.createRaw(command.newRawPassword()),
                passwordEncoder);

        userRepository.save(user);
        refreshTokenRepository.revokeAllTokensForUser(user.getId());

        // C3: Blacklist the caller's current access token immediately after save so the race
        // window between tokenVersion bump and natural token expiry is closed.
        if (command.currentAccessTokenJti() != null && command.currentAccessTokenExp() != null) {
            tokenBlacklist.revoke(command.currentAccessTokenJti(), command.userId(),
                    command.currentAccessTokenExp(), "PASSWORD_CHANGED");
        } else {
            log.warn("ChangePassword: no JTI provided, skipping access-token blacklist for userId={}",
                    command.userId());
        }
        return null;
    }
}
