package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.LoginCommand;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link LoginHandler} orchestration — credential verification,
 * username/email lookup heuristic, and token issuance. Domain rules live in {@link User};
 * here we only assert the handler wires its mocked collaborators correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginHandler (unit)")
class LoginHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenProviderPort tokenProvider;
    @Mock private TokenDomainService tokenDomainService;
    @Mock private UserDtoMapper userDtoMapper;

    private final PasswordEncoderPort encoder = new FakePasswordEncoder();

    private LoginHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginHandler(userRepository, roleRepository, refreshTokenRepository,
                encoder, tokenProvider, tokenDomainService, userDtoMapper);
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 7);
    }

    private User activeUser(String username, String email, String rawPassword) {
        return User.register(
                Username.of(username),
                Email.of(email),
                Password.createRaw(rawPassword),
                PersonName.of("Alice", "Nguyen"),
                encoder);
    }

    private void stubIssuance(User user) {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of());
        when(tokenProvider.issueAccessToken(any(User.class), anySet()))
                .thenReturn(new IssuedAccessToken("access-jwt", "jti-1", Instant.now().plusSeconds(900), 900L));
        when(tokenDomainService.issueForUser(any(User.class), anyInt()))
                .thenAnswer(inv -> RefreshToken.create(((User) inv.getArgument(0)).getId(), 7));
        // Handler calls the 2-arg toDto(User, Collection<Role>); roles arrive as a List, so match anyCollection().
        when(userDtoMapper.toDto(any(User.class), anyCollection()))
                .thenReturn(org.mockito.Mockito.mock(UserDTO.class));
    }

    @Test
    @DisplayName("identifier without '@' is resolved via findByUsername")
    void login_usesUsernameLookupWhenNoAtSign() {
        User user = activeUser("alice", "alice@example.com", "password1");
        when(userRepository.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));
        stubIssuance(user);

        AuthResponseDTO response = handler.handle(new LoginCommand("alice", "password1"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
        verify(userRepository).findByUsername(Username.of("alice"));
        verify(userRepository, never()).findByEmail(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("identifier containing '@' is resolved via findByEmail")
    void login_usesEmailLookupWhenAtSignPresent() {
        User user = activeUser("alice", "alice@example.com", "password1");
        when(userRepository.findByEmail(Email.of("alice@example.com"))).thenReturn(Optional.of(user));
        stubIssuance(user);

        handler.handle(new LoginCommand("alice@example.com", "password1"));

        verify(userRepository).findByEmail(Email.of("alice@example.com"));
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("passes the configured refreshExpiryDays to the token domain service")
    void login_passesRefreshExpiryDays() {
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 30);
        User user = activeUser("alice", "alice@example.com", "password1");
        when(userRepository.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));
        stubIssuance(user);

        handler.handle(new LoginCommand("alice", "password1"));

        verify(tokenDomainService).issueForUser(any(User.class), eq(30));
    }

    @Test
    @DisplayName("unknown user → InvalidCredentialsException with no token issued")
    void login_unknownUserThrowsInvalidCredentials() {
        when(userRepository.findByUsername(Username.of("ghost"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new LoginCommand("ghost", "password1")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("wrong password → InvalidCredentialsException, user not saved, no token issued")
    void login_wrongPasswordThrowsInvalidCredentials() {
        User user = activeUser("alice", "alice@example.com", "password1");
        when(userRepository.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> handler.handle(new LoginCommand("alice", "wrongpass1")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).save(any());
        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
    }

    @Test
    @DisplayName("blank identifier short-circuits to InvalidCredentialsException without a repo call")
    void login_blankIdentifierThrows() {
        assertThatThrownBy(() -> handler.handle(new LoginCommand("   ", "password1")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("malformed email identifier is swallowed → InvalidCredentialsException (timing parity)")
    void login_malformedEmailThrowsInvalidCredentials() {
        assertThatThrownBy(() -> handler.handle(new LoginCommand("@", "password1")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
    }

    @Test
    @DisplayName("collects role names from resolved roles and forwards them to the token provider")
    void login_forwardsRoleNamesToTokenProvider() {
        User user = activeUser("alice", "alice@example.com", "password1");
        Role roleUser = Role.create(RoleName.of("ROLE_USER"), "user");
        Role roleAdmin = Role.create(RoleName.of("ROLE_ADMIN"), "admin");
        user.assignRole(roleUser.getId());
        user.assignRole(roleAdmin.getId());

        when(userRepository.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(roleUser, roleAdmin));
        when(tokenProvider.issueAccessToken(any(User.class), anySet()))
                .thenReturn(new IssuedAccessToken("access-jwt", "jti-1", Instant.now().plusSeconds(900), 900L));
        when(tokenDomainService.issueForUser(any(User.class), anyInt()))
                .thenAnswer(inv -> RefreshToken.create(((User) inv.getArgument(0)).getId(), 7));
        when(userDtoMapper.toDto(any(User.class), anyCollection())).thenReturn(org.mockito.Mockito.mock(UserDTO.class));

        handler.handle(new LoginCommand("alice", "password1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(tokenProvider).issueAccessToken(any(User.class), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }
}
