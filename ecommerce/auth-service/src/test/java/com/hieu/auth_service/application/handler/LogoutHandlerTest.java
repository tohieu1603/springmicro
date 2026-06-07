package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.LogoutCommand;
import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import com.hieu.auth_service.domain.services.TokenProviderPort.AccessClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link LogoutHandler} — refresh-token revocation (idempotent),
 * access-token blacklisting, and graceful handling of absent/malformed tokens. All mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutHandler (unit)")
class LogoutHandlerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenProviderPort tokenProvider;
    @Mock private TokenBlacklistPort tokenBlacklist;

    private LogoutHandler handler;
    private final UserId userId = UserId.generate();

    @BeforeEach
    void setUp() {
        handler = new LogoutHandler(refreshTokenRepository, tokenProvider, tokenBlacklist);
    }

    @Test
    @DisplayName("revokes the refresh token (USER_INITIATED) and blacklists the access token")
    void logout_revokesRefreshAndBlacklistsAccess() {
        RefreshToken token = RefreshToken.create(userId, 7);
        Instant exp = Instant.now().plusSeconds(600);
        when(refreshTokenRepository.findByTokenValue(token.getValue())).thenReturn(Optional.of(token));
        when(tokenProvider.parseAccessToken("access-jwt"))
                .thenReturn(new AccessClaims("jti-9", userId.value(), "alice", 1, Set.of("ROLE_USER"), exp));

        handler.handle(new LogoutCommand("access-jwt", token.getValue().value()));

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getReason()).isEqualTo(RevokedReason.USER_INITIATED);
        verify(refreshTokenRepository).save(token);
        verify(tokenBlacklist).revoke("jti-9", userId.value(), exp, "LOGOUT");
    }

    @Test
    @DisplayName("missing refresh token in DB is a no-op for revocation but access token is still blacklisted")
    void logout_refreshTokenNotFound() {
        when(refreshTokenRepository.findByTokenValue(TokenValue.of("gone"))).thenReturn(Optional.empty());
        when(tokenProvider.parseAccessToken("access-jwt"))
                .thenReturn(new AccessClaims("jti-9", userId.value(), "alice", 1, Set.of(), Instant.now().plusSeconds(60)));

        handler.handle(new LogoutCommand("access-jwt", "gone"));

        verify(refreshTokenRepository, never()).save(any());
        verify(tokenBlacklist).revoke(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("null/blank refresh token skips the lookup entirely (cookie may legitimately be absent)")
    void logout_blankRefreshTokenSkipsLookup() {
        when(tokenProvider.parseAccessToken("access-jwt"))
                .thenReturn(new AccessClaims("jti-9", userId.value(), "alice", 1, Set.of(), Instant.now().plusSeconds(60)));

        handler.handle(new LogoutCommand("access-jwt", "   "));

        verify(refreshTokenRepository, never()).findByTokenValue(any());
        verify(tokenBlacklist).revoke(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("expired/malformed access token is swallowed — refresh revoke is not rolled back")
    void logout_malformedAccessTokenSwallowed() {
        RefreshToken token = RefreshToken.create(userId, 7);
        when(refreshTokenRepository.findByTokenValue(token.getValue())).thenReturn(Optional.of(token));
        when(tokenProvider.parseAccessToken("bad-jwt")).thenThrow(new RuntimeException("expired"));

        assertThatCode(() -> handler.handle(new LogoutCommand("bad-jwt", token.getValue().value())))
                .doesNotThrowAnyException();

        // Refresh was still revoked + saved despite the access-token failure.
        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
        verify(tokenBlacklist, never()).revoke(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("both tokens absent → completely no-op (idempotent logout of an already-out user)")
    void logout_bothTokensAbsent() {
        assertThatCode(() -> handler.handle(new LogoutCommand(null, null)))
                .doesNotThrowAnyException();

        verifyNoInteractions(refreshTokenRepository, tokenProvider, tokenBlacklist);
    }

    @Test
    @DisplayName("calling logout twice on the same refresh token is idempotent (revoke is idempotent)")
    void logout_idempotentOnRepeat() {
        RefreshToken token = RefreshToken.create(userId, 7);
        token.revoke(RevokedReason.USER_INITIATED); // already logged out once
        when(refreshTokenRepository.findByTokenValue(token.getValue())).thenReturn(Optional.of(token));

        handler.handle(new LogoutCommand(null, token.getValue().value()));

        // Still revoked, reason unchanged; save is still invoked (idempotent write).
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getReason()).isEqualTo(RevokedReason.USER_INITIATED);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(token);
        verifyNoInteractions(tokenBlacklist);
    }
}
