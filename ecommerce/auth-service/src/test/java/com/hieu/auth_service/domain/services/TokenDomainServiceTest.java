package com.hieu.auth_service.domain.services;

import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenOwnershipException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenReuseDetectedException;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link TokenDomainService} — the cross-aggregate refresh-token rules:
 * issue, rotate (with reuse detection + family revocation) and ownership validation.
 * The repository is mocked; no Spring, no DB.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenDomainService (unit)")
class TokenDomainServiceTest {

    @Mock RefreshTokenRepository repository;

    TokenDomainService service;

    @BeforeEach
    void setup() {
        service = new TokenDomainService();
    }

    private static User activeUser() {
        return User.register(
                Username.of("carol"),
                Email.of("carol@example.com"),
                Password.createRaw("password1"),
                PersonName.of("Carol", "Le"),
                new FakePasswordEncoder());
    }

    @Nested
    @DisplayName("issueForUser()")
    class IssueForUser {

        @Test
        @DisplayName("issues a generation-0 token for an authenticatable user")
        void issue_forActiveUser() {
            User user = activeUser();

            RefreshToken token = service.issueForUser(user, 7);

            assertThat(token.getGeneration().isRoot()).isTrue();
            assertThat(token.belongsTo(user.getId())).isTrue();
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("refuses to issue when the user cannot authenticate")
        void issue_forDisabledUser() {
            User user = activeUser();
            user.disable();

            assertThatThrownBy(() -> service.issueForUser(user, 7))
                    .isInstanceOf(AccountNotUsableException.class);
        }
    }

    @Nested
    @DisplayName("rotate()")
    class Rotate {

        @Test
        @DisplayName("revokes the presented token, persists it, and issues the next generation")
        void rotate_happyPath() {
            UserId userId = UserId.generate();
            RefreshToken presented = RefreshToken.create(userId, 7);

            RefreshToken rotated = service.rotate(presented, 7, repository);

            assertThat(presented.isRevoked()).isTrue();
            assertThat(presented.getReason()).isEqualTo(RevokedReason.NORMAL);
            assertThat(rotated.getFamily()).isEqualTo(presented.getFamily());
            assertThat(rotated.getGeneration().value()).isEqualTo(1);
            verify(repository).save(presented);
        }

        @Test
        @DisplayName("treats a re-presented revoked token as theft: revokes the family and throws")
        void rotate_reuseDetected_revokesFamily() {
            UserId userId = UserId.generate();
            RefreshToken presented = RefreshToken.create(userId, 7);
            presented.revoke(RevokedReason.NORMAL); // already revoked → replay

            // A still-live sibling in the same family that must be cascade-revoked.
            RefreshToken liveSibling = RefreshToken.rotate(presented, 7);
            when(repository.findByFamily(any(TokenFamily.class)))
                    .thenReturn(List.of(presented, liveSibling));

            assertThatThrownBy(() -> service.rotate(presented, 7, repository))
                    .isInstanceOf(TokenReuseDetectedException.class);

            assertThat(liveSibling.isRevoked()).isTrue();
            assertThat(liveSibling.getReason()).isEqualTo(RevokedReason.FAMILY_REVOKED);
            // The already-revoked presented token keeps its original reason and is not re-saved.
            assertThat(presented.getReason()).isEqualTo(RevokedReason.NORMAL);
            verify(repository).save(liveSibling);
            verify(repository, never()).save(presented);
        }
    }

    @Nested
    @DisplayName("revokeFamily()")
    class RevokeFamily {

        @Test
        @DisplayName("revokes only the live tokens of a family, leaving revoked ones untouched")
        void revokeFamily_onlyLiveTokens() {
            UserId userId = UserId.generate();
            RefreshToken live1 = RefreshToken.create(userId, 7);
            RefreshToken live2 = RefreshToken.rotate(live1, 7);
            RefreshToken alreadyRevoked = RefreshToken.rotate(live2, 7);
            alreadyRevoked.revoke(RevokedReason.USER_INITIATED);

            when(repository.findByFamily(any(TokenFamily.class)))
                    .thenReturn(List.of(live1, live2, alreadyRevoked));

            service.revokeFamily(live1.getFamily(), repository);

            assertThat(live1.getReason()).isEqualTo(RevokedReason.FAMILY_REVOKED);
            assertThat(live2.getReason()).isEqualTo(RevokedReason.FAMILY_REVOKED);
            assertThat(alreadyRevoked.getReason()).isEqualTo(RevokedReason.USER_INITIATED);
            verify(repository, times(2)).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("validateOwnership()")
    class ValidateOwnership {

        @Test
        @DisplayName("passes when the token belongs to the user")
        void ownership_ok() {
            UserId userId = UserId.generate();
            RefreshToken token = RefreshToken.create(userId, 7);

            assertThatCode(() -> service.validateOwnership(token, userId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws TokenOwnershipException for another user's token")
        void ownership_mismatch() {
            RefreshToken token = RefreshToken.create(UserId.generate(), 7);

            assertThatThrownBy(() -> service.validateOwnership(token, UserId.generate()))
                    .isInstanceOf(TokenOwnershipException.class);
        }
    }
}
