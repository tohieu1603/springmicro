package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.LoginWithGoogleCommand;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.OAuthEmailNotVerifiedException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.GoogleIdTokenVerifierPort;
import com.hieu.auth_service.domain.services.GoogleIdTokenVerifierPort.GoogleClaims;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link LoginWithGoogleHandler} — token verification, the
 * sub→email lookup heuristic, Google-link backfill, auto-registration of new users,
 * and the email-not-verified guard. All collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginWithGoogleHandler (unit)")
class LoginWithGoogleHandlerTest {

    @Mock private GoogleIdTokenVerifierPort googleVerifier;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenProviderPort tokenProvider;
    @Mock private UserDtoMapper userDtoMapper;

    private final PasswordEncoderPort encoder = new FakePasswordEncoder();
    private final TokenDomainService tokenDomainService = new TokenDomainService();

    private LoginWithGoogleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginWithGoogleHandler(googleVerifier, userRepository, roleRepository,
                refreshTokenRepository, encoder, tokenProvider, tokenDomainService, userDtoMapper);
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 7);
    }

    private GoogleClaims verifiedClaims() {
        return new GoogleClaims("google-sub-1", "alice@example.com", true,
                "Alice Nguyen", "Alice", "Nguyen", null);
    }

    private User existingUser() {
        return User.register(Username.of("alice"), Email.of("alice@example.com"),
                Password.createRaw("password1"), PersonName.of("Alice", "Nguyen"), encoder);
    }

    private void stubTokenIssuance() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of());
        when(tokenProvider.issueAccessToken(any(User.class), anySet()))
                .thenReturn(new IssuedAccessToken("google-access", "jti", Instant.now().plusSeconds(900), 900L));
        when(userDtoMapper.toDto(any(User.class), anyCollection())).thenReturn(org.mockito.Mockito.mock(UserDTO.class));
    }

    @Test
    @DisplayName("email not verified → OAuthEmailNotVerifiedException, no lookup or token issuance")
    void google_emailNotVerified() {
        when(googleVerifier.verify("raw")).thenReturn(new GoogleClaims(
                "google-sub-1", "alice@example.com", false, "Alice", "Alice", "Nguyen", null));

        assertThatThrownBy(() -> handler.handle(new LoginWithGoogleCommand("raw")))
                .isInstanceOf(OAuthEmailNotVerifiedException.class);

        verify(userRepository, never()).findByGoogleSub(any());
        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
    }

    @Test
    @DisplayName("user found by google sub → tokens issued, no new registration")
    void google_foundBySub() {
        GoogleClaims claims = verifiedClaims();
        User user = existingUser();
        user.linkGoogleAccount(GoogleSub.of(claims.sub()));
        when(googleVerifier.verify("raw")).thenReturn(claims);
        when(userRepository.findByGoogleSub(GoogleSub.of(claims.sub()))).thenReturn(Optional.of(user));
        stubTokenIssuance();

        AuthResponseDTO response = handler.handle(new LoginWithGoogleCommand("raw"));

        assertThat(response.accessToken()).isEqualTo("google-access");
        verify(userRepository, never()).findByEmail(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("found by email when sub misses → links the Google account onto the existing user")
    void google_foundByEmailLinksAccount() {
        GoogleClaims claims = verifiedClaims();
        User user = existingUser(); // no google sub yet
        when(googleVerifier.verify("raw")).thenReturn(claims);
        when(userRepository.findByGoogleSub(GoogleSub.of(claims.sub()))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(Email.of(claims.email()))).thenReturn(Optional.of(user));
        stubTokenIssuance();

        handler.handle(new LoginWithGoogleCommand("raw"));

        assertThat(user.isLinkedWithGoogle()).isTrue();
        assertThat(user.getGoogleSub()).isEqualTo(GoogleSub.of(claims.sub()));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("no existing user → auto-registers a new Google account with the default user role")
    void google_autoRegistersNewUser() {
        GoogleClaims claims = verifiedClaims();
        Role defaultRole = Role.create(RoleName.of("ROLE_USER"), "default users");
        when(googleVerifier.verify("raw")).thenReturn(claims);
        when(userRepository.findByGoogleSub(GoogleSub.of(claims.sub()))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(Email.of(claims.email()))).thenReturn(Optional.empty());
        when(userRepository.isUsernameTaken(any())).thenReturn(false); // "alice" is free
        when(roleRepository.findByName(RoleName.of("ROLE_USER"))).thenReturn(Optional.of(defaultRole));
        stubTokenIssuance();

        handler.handle(new LoginWithGoogleCommand("raw"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User created = saved.getValue();
        assertThat(created.getUsername()).isEqualTo(Username.of("alice"));
        assertThat(created.getEmail()).isEqualTo(Email.of("alice@example.com"));
        assertThat(created.isLinkedWithGoogle()).isTrue();
        assertThat(created.getRoles()).containsExactly(defaultRole.getId());
    }

    @Test
    @DisplayName("derives a numeric-suffixed username when the base candidate is already taken")
    void google_derivesSuffixedUsernameOnCollision() {
        GoogleClaims claims = verifiedClaims();
        when(googleVerifier.verify("raw")).thenReturn(claims);
        when(userRepository.findByGoogleSub(GoogleSub.of(claims.sub()))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(Email.of(claims.email()))).thenReturn(Optional.empty());
        // "alice" taken, "alice1" free.
        when(userRepository.isUsernameTaken(Username.of("alice"))).thenReturn(true);
        when(userRepository.isUsernameTaken(Username.of("alice1"))).thenReturn(false);
        lenient().when(roleRepository.findByName(RoleName.of("ROLE_USER"))).thenReturn(Optional.empty());
        stubTokenIssuance();

        handler.handle(new LoginWithGoogleCommand("raw"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo(Username.of("alice1"));
    }

    @Test
    @DisplayName("forwards the configured refreshExpiryDays to the refresh token created on Google login")
    void google_usesConfiguredExpiryDays() {
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 3);
        GoogleClaims claims = verifiedClaims();
        User user = existingUser();
        user.linkGoogleAccount(GoogleSub.of(claims.sub()));
        when(googleVerifier.verify("raw")).thenReturn(claims);
        when(userRepository.findByGoogleSub(GoogleSub.of(claims.sub()))).thenReturn(Optional.of(user));
        stubTokenIssuance();

        handler.handle(new LoginWithGoogleCommand("raw"));

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        long remaining = saved.getValue().getRemainingSeconds();
        assertThat(remaining).isBetween(2L * 24 * 3600, 3L * 24 * 3600 + 60);
    }
}
