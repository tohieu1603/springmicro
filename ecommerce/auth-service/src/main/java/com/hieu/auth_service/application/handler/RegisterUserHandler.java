package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.RegisterUserCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserAlreadyExistsException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.domain.services.TokenDomainService;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles {@link RegisterUserCommand}: creates the user aggregate, assigns a default role,
 * persists it, and issues an initial access + refresh token pair.
 */
@Service
@RequiredArgsConstructor
public class RegisterUserHandler implements CommandHandler<RegisterUserCommand, AuthResponseDTO> {

    /** Default role granted to every new user. Must already exist in the role registry. */
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final TokenDomainService tokenDomainService;
    private final UserDtoMapper userDtoMapper;

    @Value("${jwt.refresh-expiration-days:7}")
    private int refreshExpiryDays;

    /**
     * Executes the registration use case.
     *
     * @param command immutable input describing the new user
     * @return access + refresh tokens wrapped with the user's profile
     * @throws UserAlreadyExistsException if username or email is already in use
     */
    @Override
    @Transactional
    public AuthResponseDTO handle(RegisterUserCommand command) {
        Username username = Username.of(command.username());
        Email email = Email.of(command.email());

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username=" + command.username());
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email=" + command.email());
        }

        User user = User.register(
                username,
                email,
                Password.createRaw(command.rawPassword()),
                PersonName.of(command.firstName(), command.lastName()),
                passwordEncoder);

        roleRepository.findByName(RoleName.of(DEFAULT_ROLE))
                .ifPresent(role -> user.assignRole(role.getId()));

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
}
