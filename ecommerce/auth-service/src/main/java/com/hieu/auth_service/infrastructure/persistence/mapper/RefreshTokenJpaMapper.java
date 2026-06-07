package com.hieu.auth_service.infrastructure.persistence.mapper;

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
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer between the {@code RefreshToken} aggregate and {@link RefreshTokenJpaEntity}.
 *
 * <p>JPA requires the {@code @ManyToOne} user relationship to be a managed entity —
 * the caller resolves the {@link UserJpaEntity} reference and hands it here so the
 * mapper stays free of repository dependencies.
 */
@Component
public class RefreshTokenJpaMapper {

    /**
     * Projects a domain {@link RefreshToken} onto a persistable JPA entity.
     *
     * @param token     source aggregate
     * @param userRef   JPA user entity the token belongs to (must be managed)
     * @param isNew     whether this is an INSERT path
     * @return populated JPA entity
     */
    public RefreshTokenJpaEntity toJpaEntity(RefreshToken token, UserJpaEntity userRef, boolean isNew) {
        if (token == null) return null;
        return RefreshTokenJpaEntity.builder()
                .id(token.getId().value())
                .token(token.getValue().value())
                .user(userRef)
                .expiryDate(token.getExpiry().expiryDate())
                .revoked(token.isRevoked())
                .revokedAt(token.getRevokedAt())
                .family(token.getFamily().value())
                .generation(token.getGeneration().value())
                .revokedReason(token.getReason() != null ? token.getReason().value() : null)
                .createdAt(token.getCreatedAt())
                .isNew(isNew)
                .build();
    }

    /** Rebuilds a domain {@link RefreshToken} from its JPA representation. */
    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        if (entity == null) return null;
        return RefreshToken.reconstitute(
                TokenId.of(entity.getId()),
                TokenValue.of(entity.getToken()),
                UserId.of(entity.getUser().getId()),
                TokenExpiry.of(entity.getExpiryDate()),
                Boolean.TRUE.equals(entity.getRevoked()),
                entity.getCreatedAt(),
                entity.getRevokedAt(),
                TokenFamily.of(entity.getFamily()),
                GenerationNumber.of(entity.getGeneration()),
                entity.getRevokedReason() != null ? RevokedReason.of(entity.getRevokedReason()) : null);
    }
}
