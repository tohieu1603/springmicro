package com.hieu.auth_service.domain.models.refreshtoken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hieu.auth_service.domain.events.DomainEvent;
import com.hieu.auth_service.domain.models.refreshtoken.events.TokenCreatedEvent;
import com.hieu.auth_service.domain.models.refreshtoken.events.TokenRevokedEvent;
import com.hieu.auth_service.domain.models.refreshtoken.events.TokenRotatedEvent;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenExpiredException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenRevokedException;
import com.hieu.auth_service.domain.models.refreshtoken.vo.GenerationNumber;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenExpiry;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenId;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.models.user.vo.UserId;

/**
 * Pure unit tests for the {@link RefreshToken} aggregate — rotation, reuse detection,
 * revocation idempotency, validity, and domain-event registration. No Spring, no DB.
 */
@DisplayName("RefreshToken aggregate (unit)")
class RefreshTokenTest {

    private static UserId user() {
        return UserId.generate();
    }

    /** Builds a token with an arbitrary (revoked/expiry) state without waiting on the clock. */
    private static RefreshToken reconstituted(UserId userId, Instant expiry, boolean revoked,
                                              GenerationNumber generation, RevokedReason reason) {
        return RefreshToken.reconstitute(
                TokenId.generate(), TokenValue.generate(), userId,
                TokenExpiry.of(expiry), revoked,
                Instant.now().minusSeconds(120), revoked ? Instant.now() : null,
                TokenFamily.generate(), generation, reason);
    }

    @Nested
    @DisplayName("create() — root token of a new login session")
    class Create {

        @Test
        @DisplayName("starts at generation 0 with a fresh family, valid and not revoked")
        void create_rootGenerationFreshFamilyValid() {
            UserId userId = user();

            RefreshToken token = RefreshToken.create(userId, 7);

            assertThat(token.getGeneration()).isEqualTo(GenerationNumber.root());
            assertThat(token.getGeneration().isRoot()).isTrue();
            assertThat(token.getFamily()).isNotNull();
            assertThat(token.isRevoked()).isFalse();
            assertThat(token.isValid()).isTrue();
            assertThat(token.isReuseAttempt()).isFalse();
            assertThat(token.belongsTo(userId)).isTrue();
            assertThat(token.getRemainingSeconds()).isPositive();
        }

        @Test
        @DisplayName("registers a single TokenCreatedEvent")
        void create_registersCreatedEvent() {
            RefreshToken token = RefreshToken.create(user(), 7);

            List<DomainEvent> events = token.peekDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(TokenCreatedEvent.class);
        }
    }

    @Nested
    @DisplayName("rotate() — next-generation token in the same family")
    class Rotate {

        @Test
        @DisplayName("keeps the family and increments the generation")
        void rotate_sameFamilyNextGeneration() {
            RefreshToken root = RefreshToken.create(user(), 7);

            RefreshToken next = RefreshToken.rotate(root, 7);

            assertThat(next.getFamily()).isEqualTo(root.getFamily());
            assertThat(next.getGeneration().value()).isEqualTo(root.getGeneration().value() + 1);
            assertThat(next.getId()).isNotEqualTo(root.getId());
            assertThat(next.getValue()).isNotEqualTo(root.getValue());
            assertThat(next.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("registers a TokenRotatedEvent")
        void rotate_registersRotatedEvent() {
            RefreshToken next = RefreshToken.rotate(RefreshToken.create(user(), 7), 7);

            assertThat(next.peekDomainEvents()).hasSize(1);
            assertThat(next.peekDomainEvents().get(0)).isInstanceOf(TokenRotatedEvent.class);
        }
    }

    @Nested
    @DisplayName("verifyValidity()")
    class VerifyValidity {

        @Test
        @DisplayName("does not throw for a fresh token")
        void freshToken_noThrow() {
            RefreshToken token = RefreshToken.create(user(), 7);
            assertThatCode(token::verifyValidity).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws TokenRevokedException for a revoked token")
        void revokedToken_throwsRevoked() {
            RefreshToken token = RefreshToken.create(user(), 7);
            token.revoke(RevokedReason.USER_INITIATED);

            assertThatThrownBy(token::verifyValidity)
                    .isInstanceOf(TokenRevokedException.class);
        }

        @Test
        @DisplayName("throws TokenExpiredException for an expired (non-revoked) token")
        void expiredToken_throwsExpired() {
            RefreshToken expired = reconstituted(
                    user(), Instant.now().minusSeconds(60), false,
                    GenerationNumber.root(), null);

            assertThatThrownBy(expired::verifyValidity)
                    .isInstanceOf(TokenExpiredException.class);
        }
    }

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("sets revoked + reason and registers a TokenRevokedEvent")
        void revoke_setsStateAndEvent() {
            RefreshToken token = RefreshToken.create(user(), 7);

            token.revoke(RevokedReason.FAMILY_REVOKED);

            assertThat(token.isRevoked()).isTrue();
            assertThat(token.getReason()).isEqualTo(RevokedReason.FAMILY_REVOKED);
            assertThat(token.isReuseAttempt()).isTrue();
            assertThat(token.peekDomainEvents())
                    .anyMatch(e -> e instanceof TokenRevokedEvent);
        }

        @Test
        @DisplayName("is idempotent — a second revoke keeps the original reason and adds no event")
        void revoke_idempotent() {
            RefreshToken token = RefreshToken.create(user(), 7);

            token.revoke(RevokedReason.NORMAL);
            long revokeEventsAfterFirst = token.peekDomainEvents().stream()
                    .filter(e -> e instanceof TokenRevokedEvent).count();

            token.revoke(RevokedReason.REUSE_DETECTED); // ignored — already revoked

            assertThat(token.getReason()).isEqualTo(RevokedReason.NORMAL);
            long revokeEventsAfterSecond = token.peekDomainEvents().stream()
                    .filter(e -> e instanceof TokenRevokedEvent).count();
            assertThat(revokeEventsAfterSecond).isEqualTo(revokeEventsAfterFirst);
        }

        @Test
        @DisplayName("revoked tokens report 0 remaining seconds")
        void revoked_zeroRemaining() {
            RefreshToken token = RefreshToken.create(user(), 7);
            token.revoke();
            assertThat(token.getRemainingSeconds()).isZero();
        }
    }

    @Nested
    @DisplayName("query helpers")
    class Queries {

        @Test
        @DisplayName("wasRevokedForSecurity() is true only for REUSE_DETECTED / FAMILY_REVOKED")
        void wasRevokedForSecurity() {
            RefreshToken normal = RefreshToken.create(user(), 7);
            normal.revoke(RevokedReason.NORMAL);
            assertThat(normal.wasRevokedForSecurity()).isFalse();

            RefreshToken stolen = RefreshToken.create(user(), 7);
            stolen.revoke(RevokedReason.FAMILY_REVOKED);
            assertThat(stolen.wasRevokedForSecurity()).isTrue();
        }

        @Test
        @DisplayName("belongsTo() distinguishes the owner from another user")
        void belongsTo() {
            UserId owner = user();
            RefreshToken token = RefreshToken.create(owner, 7);

            assertThat(token.belongsTo(owner)).isTrue();
            assertThat(token.belongsTo(user())).isFalse();
        }

        @Test
        @DisplayName("willExpireSoon() honours the threshold")
        void willExpireSoon() {
            RefreshToken token = RefreshToken.create(user(), 7); // ~7 days out

            assertThat(token.willExpireSoon(60)).isFalse();
            assertThat(token.willExpireSoon(30L * 24 * 3600)).isTrue();
        }
    }
}
