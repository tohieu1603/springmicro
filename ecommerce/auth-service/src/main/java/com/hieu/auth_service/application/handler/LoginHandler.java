package com.hieu.auth_service.application.handler;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hieu.auth_service.application.command.LoginCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.domain.services.TokenDomainService;
import com.hieu.auth_service.domain.services.TokenProviderPort;

import lombok.RequiredArgsConstructor;

/**
 * Handles {@link LoginCommand}: verifies credentials, issues a new access+refresh pair.
 *
 * <p>Delegates account-status checks to the {@code User} aggregate so the domain rule stays
 * in a single place. Wrong credentials and unusable accounts surface as distinct exceptions.
 */
@Service
@RequiredArgsConstructor
public class LoginHandler implements CommandHandler<LoginCommand, AuthResponseDTO> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final TokenDomainService tokenDomainService;
    private final UserDtoMapper userDtoMapper;

    @Value("${jwt.refresh-expiration-days:7}")
    private int refreshExpiryDays;

    @Override
    @Transactional
    public AuthResponseDTO handle(LoginCommand command) {
        User user = lookup(command.usernameOrEmail())
                .orElseThrow(InvalidCredentialsException::new);

        boolean ok = user.authenticate(Password.createRaw(command.rawPassword()), passwordEncoder);
        if (!ok) {
            throw new InvalidCredentialsException();
        }
        User saved = userRepository.save(user);

        List<Role> userRoles = roleRepository.findByIdIn(saved.getRoles());
        Set<String> roleNames = userRoles.stream()
                .map(r -> r.getName().value())
                .collect(Collectors.toSet());

        var issued = tokenProvider.issueAccessToken(saved, roleNames);
        RefreshToken refresh = tokenDomainService.issueForUser(saved, refreshExpiryDays);
        refreshTokenRepository.save(refresh);

        return AuthResponseDTO.bearer(
                issued.token(),
                refresh.getValue().value(),
                issued.expiresInSeconds(),
                userDtoMapper.toDto(saved, userRoles));
    }

    /**
     * Resolves the user by username or email using a simple "{@code @}" heuristic.
     * Falls back to empty {@link Optional} on malformed input — the caller then raises
     * {@link InvalidCredentialsException} so timing is identical to "user not found".
     */
    private Optional<User> lookup(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) return Optional.empty();
        try {
            return usernameOrEmail.contains("@")
                    ? userRepository.findByEmail(Email.of(usernameOrEmail))
                    : userRepository.findByUsername(Username.of(usernameOrEmail));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }
}
