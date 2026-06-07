package com.hieu.auth_service.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.vo.GenerationNumber;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenExpiry;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenId;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RefreshTokenJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.UserJpaEntity;

/**
 * Pure unit tests for {@link RefreshTokenJpaMapper}: aggregate <-> entity mapping including the
 * managed {@code @ManyToOne} user reference, revoked-reason null handling, generation/family
 * value objects, and round-trip identity. No persistence context.
 */
class RefreshTokenJpaMapperTest {

    private final RefreshTokenJpaMapper mapper = new RefreshTokenJpaMapper();

    private static final String TOKEN_ID = "44444444-4444-4444-4444-444444444444";
    private static final String FAMILY = "55555555-5555-5555-5555-555555555555";
    private static final String USER_ID = "66666666-6666-6666-6666-666666666666";

    private RefreshToken domainToken(boolean revoked, RevokedReason reason) {
        return RefreshToken.reconstitute(
                TokenId.of(TOKEN_ID),
                TokenValue.of("opaque-token-value"),
                UserId.of(USER_ID),
                TokenExpiry.of(Instant.parse("2030-01-01T00:00:00Z")),
                revoked,
                Instant.parse("2024-01-01T00:00:00Z"),
                revoked ? Instant.parse("2024-05-05T00:00:00Z") : null,
                TokenFamily.of(FAMILY),
                GenerationNumber.of(2),
                reason);
    }

    private UserJpaEntity userRef() {
        return UserJpaEntity.builder().id(USER_ID).username("dave").email("dave@example.com").build();
    }

    @Test
    void toJpaEntity_mapsAllFields() {
        RefreshTokenJpaEntity entity =
                mapper.toJpaEntity(domainToken(true, RevokedReason.REUSE_DETECTED), userRef(), false);

        assertThat(entity.getId()).isEqualTo(TOKEN_ID);
        assertThat(entity.getToken()).isEqualTo("opaque-token-value");
        assertThat(entity.getUser().getId()).isEqualTo(USER_ID);
        assertThat(entity.getExpiryDate()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
        assertThat(entity.getRevoked()).isTrue();
        assertThat(entity.getRevokedAt()).isEqualTo(Instant.parse("2024-05-05T00:00:00Z"));
        assertThat(entity.getFamily()).isEqualTo(FAMILY);
        assertThat(entity.getGeneration()).isEqualTo(2);
        assertThat(entity.getRevokedReason()).isEqualTo("REUSE_DETECTED");
        assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void toJpaEntity_nullReasonMapsToNull() {
        RefreshTokenJpaEntity entity = mapper.toJpaEntity(domainToken(false, null), userRef(), true);

        assertThat(entity.getRevokedReason()).isNull();
        assertThat(entity.getRevoked()).isFalse();
        assertThat(entity.getRevokedAt()).isNull();
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void toDomain_reconstitutesAggregate() {
        RefreshTokenJpaEntity entity =
                mapper.toJpaEntity(domainToken(true, RevokedReason.NORMAL), userRef(), false);

        RefreshToken domain = mapper.toDomain(entity);

        assertThat(domain.getId().value()).isEqualTo(TOKEN_ID);
        assertThat(domain.getValue().value()).isEqualTo("opaque-token-value");
        assertThat(domain.getUserId().value()).isEqualTo(USER_ID);
        assertThat(domain.getFamily().value()).isEqualTo(FAMILY);
        assertThat(domain.getGeneration().value()).isEqualTo(2);
        assertThat(domain.isRevoked()).isTrue();
        assertThat(domain.getReason()).isEqualTo(RevokedReason.NORMAL);
    }

    @Test
    void toDomain_nullRevokedReasonStaysNull() {
        RefreshTokenJpaEntity entity =
                mapper.toJpaEntity(domainToken(false, null), userRef(), true);

        RefreshToken domain = mapper.toDomain(entity);

        assertThat(domain.getReason()).isNull();
        assertThat(domain.isRevoked()).isFalse();
    }

    @Test
    void toDomain_nullRevokedFlagTreatedAsFalse() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(TOKEN_ID)
                .token("t")
                .user(userRef())
                .expiryDate(Instant.parse("2030-01-01T00:00:00Z"))
                .revoked(null)
                .createdAt(Instant.now())
                .family(FAMILY)
                .generation(0)
                .isNew(false)
                .build();

        // builder normalises null revoked -> false; mapper's Boolean.TRUE.equals guard holds.
        RefreshToken domain = mapper.toDomain(entity);

        assertThat(domain.isRevoked()).isFalse();
    }

    @Test
    void roundTrip_preservesState() {
        RefreshToken original = domainToken(true, RevokedReason.FAMILY_REVOKED);

        RefreshToken rebuilt = mapper.toDomain(mapper.toJpaEntity(original, userRef(), false));

        assertThat(rebuilt.getId()).isEqualTo(original.getId());
        assertThat(rebuilt.getFamily()).isEqualTo(original.getFamily());
        assertThat(rebuilt.getGeneration()).isEqualTo(original.getGeneration());
        assertThat(rebuilt.isRevoked()).isEqualTo(original.isRevoked());
        assertThat(rebuilt.getReason()).isEqualTo(original.getReason());
    }

    @Test
    void nullInputs_returnNull() {
        assertThat(mapper.toJpaEntity(null, userRef(), true)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
