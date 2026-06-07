package com.hieu.auth_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Blacklist entry for a revoked access token.
 *
 * <p>Primary key is the JWT jti claim. Extends {@link BaseManualIdEntity} so Hibernate
 * honours the {@code Persistable<String>} contract — otherwise Spring Data would
 * issue a {@code SELECT} before every INSERT (manual-id entities appear "detached" by default).
 *
 * <p>Rows are purged hourly once the underlying token expires naturally.
 */
@Entity
@Table(name = "token_revocations", indexes = {
        @Index(name = "idx_token_revocations_user_id", columnList = "user_id"),
        @Index(name = "idx_token_revocations_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationJpaEntity extends BaseManualIdEntity {

    /** Owning user id (UUID as String) — same type as the User aggregate identifier. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** Timestamp the revocation was recorded. */
    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    /** Original JWT expiry — drives Redis TTL + cleanup. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Free-form reason: LOGOUT, ADMIN_REVOKE, SECURITY_BREACH, PASSWORD_CHANGED. */
    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    /**
     * Convenience factory so callers don't have to remember {@code setIsNew(true)}.
     *
     * @param tokenId    JWT jti (primary key)
     * @param userId     owning user UUID
     * @param expiresAt  original JWT expiry
     * @param reason     reason code
     * @return populated entity ready for {@code save()}
     */
    public static TokenRevocationJpaEntity forRevocation(String tokenId, String userId,
                                                         Instant expiresAt, String reason) {
        TokenRevocationJpaEntity entity = new TokenRevocationJpaEntity();
        entity.id = tokenId;
        entity.userId = userId;
        entity.createdAt = Instant.now();
        entity.revokedAt = Instant.now();
        entity.expiresAt = expiresAt;
        entity.reason = reason;
        entity.isNew = true;
        return entity;
    }
}
