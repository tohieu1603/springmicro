package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.ChangeAccountStatusCommand;
import com.hieu.auth_service.application.command.ChangeAccountStatusCommand.Transition;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link ChangeAccountStatusCommand}. Dispatches to the aggregate method matching
 * the transition enum — a small switch keeps handler count low while each transition
 * still raises its own domain event via {@link User}'s status mutators.
 *
 * <p>Locking or disabling an account must take effect <em>immediately</em>, not at the next
 * natural token expiry: the access-token {@code tokenVersion} is bumped (invalidating every
 * outstanding JWT) and all refresh tokens are revoked (so the session cannot be rotated back
 * to life). The {@link com.hieu.auth_service.interfaces.rest.filter.JwtAuthenticationFilter}
 * also re-checks account status per request as a second line of defence.
 */
@Service
@RequiredArgsConstructor
public class ChangeAccountStatusHandler implements CommandHandler<ChangeAccountStatusCommand, Void> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public Void handle(ChangeAccountStatusCommand command) {
        User user = userRepository.findById(UserId.of(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        Transition transition = command.transition();
        switch (transition) {
            case LOCK    -> user.lock();
            case UNLOCK  -> user.unlock();
            case ENABLE  -> user.enable();
            case DISABLE -> user.disable();
        }

        // A transition that makes the account unusable must terminate live sessions now.
        if (transition == Transition.LOCK || transition == Transition.DISABLE) {
            user.incrementTokenVersion();
            refreshTokenRepository.revokeAllTokensForUser(user.getId());
        }

        userRepository.save(user);
        return null;
    }
}
