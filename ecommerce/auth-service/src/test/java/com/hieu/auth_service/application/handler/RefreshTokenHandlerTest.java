package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.RefreshTokenCommand;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenReuseDetectedException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenRevokedException;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.TokenDomainService;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import com.hieu.auth_service.domain.services.TokenProviderPort.IssuedAccessToken;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link RefreshTokenHandler}. Rotation + reuse-detection rules live in
 * {@link TokenDomainService}; here we verify the handler's orchestration: pessimistic lookup,
 * rotated-token persistence, re-issuance of the access token, and reuse → exception propagation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenHandler (unit)")
class RefreshTokenHandlerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private TokenProviderPort tokenProvider;
    @Mock private UserDtoMapper userDtoMapper;

    private final FakePasswordEncoder encoder = new FakePasswordEncoder();
    // Use the real domain service so the rotation/reuse rules are genuinely exercised.
    private final TokenDomainService tokenDomainService = new TokenDomainService();

    private RefreshTokenHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        handler = new RefreshTokenHandler(refreshTokenRepository, userRepository, roleRepository,
                tokenDomainService, tokenProvider, userDtoMapper);
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 7);
        user = User.register(Username.of("alice"), Email.of("alice@example.com"),
                Password.createRaw("password1"), PersonName.of("Alice", "Nguyen"), encoder);
    }

    @Test
    @DisplayName("happy path rotates the presented token, saves the rotated one, and re-issues access")
    void refresh_happyPathRotates() {
        RefreshToken presented = RefreshToken.create(user.getId(), 7);
        when(refreshTokenRepository.findByTokenValueForUpdate(presented.getValue()))
                .thenReturn(Optional.of(presented));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of());
        when(tokenProvider.issueAccessToken(any(User.class), anySet()))
                .thenReturn(new IssuedAccessToken("new-access", "jti-2", Instant.now().plusSeconds(900), 900L));
        when(userDtoMapper.toDto(any(User.class), anyCollection())).thenReturn(org.mockito.Mockito.mock(UserDTO.class));

        AuthResponseDTO response = handler.handle(
                new RefreshTokenCommand(presented.getValue().value()));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isNotEqualTo(presented.getValue().value());
        // Old token revoked (NORMAL) by the domain service and rotated token saved → 2 saves.
        assertThat(presented.isRevoked()).isTrue();
        assertThat(presented.getReason()).isEqualTo(RevokedReason.NORMAL);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("unknown token value → TokenRevokedException")
    void refresh_unknownTokenThrows() {
        when(refreshTokenRepository.findByTokenValueForUpdate(TokenValue.of("missing-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new RefreshTokenCommand("missing-token")))
                .isInstanceOf(TokenRevokedException.class);

        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
    }

    @Test
    @DisplayName("replaying an already-revoked token revokes the family and throws TokenReuseDetectedException")
    void refresh_reuseDetected() {
        RefreshToken stolen = RefreshToken.create(user.getId(), 7);
        stolen.revoke(RevokedReason.NORMAL); // already rotated away → reuse signal
        when(refreshTokenRepository.findByTokenValueForUpdate(stolen.getValue()))
                .thenReturn(Optional.of(stolen));
        // Family revocation walks the family; return only the (already-revoked) stolen token.
        when(refreshTokenRepository.findByFamily(stolen.getFamily())).thenReturn(List.of(stolen));

        assertThatThrownBy(() -> handler.handle(new RefreshTokenCommand(stolen.getValue().value())))
                .isInstanceOf(TokenReuseDetectedException.class);

        // No new access token is issued and no rotated token is saved on a reuse attempt.
        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("user gone after rotation → UserNotFoundException")
    void refresh_userNotFound() {
        RefreshToken presented = RefreshToken.create(user.getId(), 7);
        when(refreshTokenRepository.findByTokenValueForUpdate(presented.getValue()))
                .thenReturn(Optional.of(presented));
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new RefreshTokenCommand(presented.getValue().value())))
                .isInstanceOf(UserNotFoundException.class);

        verify(tokenProvider, never()).issueAccessToken(any(), anySet());
    }

    @Test
    @DisplayName("forwards the configured refreshExpiryDays to the rotated token's expiry window")
    void refresh_usesConfiguredExpiryDays() {
        ReflectionTestUtils.setField(handler, "refreshExpiryDays", 2);
        RefreshToken presented = RefreshToken.create(user.getId(), 7);
        when(refreshTokenRepository.findByTokenValueForUpdate(presented.getValue()))
                .thenReturn(Optional.of(presented));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of());
        when(tokenProvider.issueAccessToken(any(User.class), anySet()))
                .thenReturn(new IssuedAccessToken("new-access", "jti-2", Instant.now().plusSeconds(900), 900L));
        when(userDtoMapper.toDto(any(User.class), anyCollection())).thenReturn(org.mockito.Mockito.mock(UserDTO.class));

        handler.handle(new RefreshTokenCommand(presented.getValue().value()));

        // ~2 days remaining (allow generous slack) confirms expiryDays propagated.
        org.mockito.ArgumentCaptor<RefreshToken> saved = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        RefreshToken rotated = saved.getAllValues().stream().filter(t -> !t.isRevoked()).findFirst().orElseThrow();
        long remaining = rotated.getRemainingSeconds();
        assertThat(remaining).isBetween(1L, 2L * 24 * 3600 + 60);
        assertThat(remaining).isGreaterThan(24 * 3600); // strictly more than a day → used 2, not a default
    }
}
