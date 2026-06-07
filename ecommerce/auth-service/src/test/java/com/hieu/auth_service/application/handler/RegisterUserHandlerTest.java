package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.RegisterUserCommand;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserAlreadyExistsException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.domain.services.TokenDomainService;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import com.hieu.auth_service.domain.services.TokenProviderPort.IssuedAccessToken;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link RegisterUserHandler} — uniqueness guards, default-role
 * assignment, persistence, and initial token issuance. All collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserHandler (unit)")
class RegisterUserHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenProviderPort tokenProvider;
    @Mock private TokenDomainService tokenDomainService;
    @Mock private UserDtoMapper userDtoMapper;

    private final PasswordEncoderPort encoder = new FakePasswordEncoder();

    private RegisterUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterUserHandler(userRepository, roleRepository, refreshTokenRepository,
                encoder, tokenProvider, tokenDomainService, userDtoMapper);
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 7);
    }

    private RegisterUserCommand cmd() {
        return new RegisterUserCommand("alice", "alice@example.com", "password1", "Alice", "Nguyen");
    }

    private void stubIssuance() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of());
        when(tokenProvider.issueAccessToken(any(User.class), anySet()))
                .thenReturn(new IssuedAccessToken("access-jwt", "jti-1", Instant.now().plusSeconds(900), 900L));
        when(tokenDomainService.issueForUser(any(User.class), anyInt()))
                .thenAnswer(inv -> RefreshToken.create(((User) inv.getArgument(0)).getId(), 7));
        when(userDtoMapper.toDto(any(User.class), anyCollection()))
                .thenReturn(org.mockito.Mockito.mock(UserDTO.class));
    }

    @Test
    @DisplayName("happy path hashes password, assigns default role, persists and issues tokens")
    void register_happyPath() {
        Role defaultRole = Role.create(RoleName.of("ROLE_USER"), "default user role");
        when(userRepository.existsByUsername(Username.of("alice"))).thenReturn(false);
        when(userRepository.existsByEmail(Email.of("alice@example.com"))).thenReturn(false);
        when(roleRepository.findByName(RoleName.of("ROLE_USER"))).thenReturn(Optional.of(defaultRole));
        stubIssuance();

        AuthResponseDTO response = handler.handle(cmd());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.refreshToken()).isNotBlank();

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRoles()).containsExactly(defaultRole.getId());
        assertThat(savedUser.getValue().getPassword().value()).isEqualTo("ENC(password1)");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("missing default role still registers the user (role assignment is best-effort)")
    void register_missingDefaultRole() {
        when(userRepository.existsByUsername(Username.of("alice"))).thenReturn(false);
        when(userRepository.existsByEmail(Email.of("alice@example.com"))).thenReturn(false);
        when(roleRepository.findByName(RoleName.of("ROLE_USER"))).thenReturn(Optional.empty());
        stubIssuance();

        handler.handle(cmd());

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRoles()).isEmpty();
    }

    @Test
    @DisplayName("duplicate username → UserAlreadyExistsException before any persistence")
    void register_duplicateUsername() {
        when(userRepository.existsByUsername(Username.of("alice"))).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(cmd()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username=alice");

        verify(userRepository, never()).save(any());
        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
    }

    @Test
    @DisplayName("duplicate email → UserAlreadyExistsException before any persistence")
    void register_duplicateEmail() {
        when(userRepository.existsByUsername(Username.of("alice"))).thenReturn(false);
        when(userRepository.existsByEmail(Email.of("alice@example.com"))).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(cmd()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email=alice@example.com");

        verify(userRepository, never()).save(any());
        verify(tokenDomainService, never()).issueForUser(any(), anyInt());
    }

    @Test
    @DisplayName("passes the configured refreshExpiryDays through to the token domain service")
    void register_passesRefreshExpiryDays() {
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 14);
        lenient().when(roleRepository.findByName(RoleName.of("ROLE_USER"))).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(Username.of("alice"))).thenReturn(false);
        when(userRepository.existsByEmail(Email.of("alice@example.com"))).thenReturn(false);
        stubIssuance();

        handler.handle(cmd());

        verify(tokenDomainService).issueForUser(any(User.class), org.mockito.ArgumentMatchers.eq(14));
    }
}
