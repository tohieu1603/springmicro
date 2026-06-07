package com.hieu.auth_service.infrastructure.persistence.jpa.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RefreshTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    Optional<RefreshTokenJpaEntity> findByToken(String token);

    /**
     * Locks the token row so two concurrent refreshes of the same token serialize —
     * the second one sees the now-revoked token and triggers family-revocation reuse
     * detection instead of both successfully issuing access tokens.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshTokenJpaEntity t WHERE t.token = :token")
    Optional<RefreshTokenJpaEntity> findByTokenForUpdate(@Param("token") String token);

    List<RefreshTokenJpaEntity> findByUserId(String userId);

    @Query("SELECT t FROM RefreshTokenJpaEntity t WHERE t.user.id = :userId AND t.revoked = false AND t.expiryDate > :now")
    List<RefreshTokenJpaEntity> findValidTokensByUserId(@Param("userId") String userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshTokenJpaEntity t WHERE t.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    // H3: Persist the revocation reason so isReuseAttempt() can distinguish normal vs security revokes.
    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true, t.revokedAt = :now, t.revokedReason = 'PASSWORD_CHANGED' WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllTokensForUser(@Param("userId") String userId, @Param("now") Instant now);

    void deleteByUserId(String userId);

    List<RefreshTokenJpaEntity> findByFamily(String family);
}