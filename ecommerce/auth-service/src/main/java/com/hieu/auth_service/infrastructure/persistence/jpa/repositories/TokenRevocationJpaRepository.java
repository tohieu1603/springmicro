package com.hieu.auth_service.infrastructure.persistence.jpa.repositories;

import com.hieu.auth_service.infrastructure.persistence.jpa.entities.TokenRevocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * JPA repository for the token_revocations table.
 * Provides fast existence checks, Redis warm-up queries, and cleanup operations.
 */
@Repository
public interface TokenRevocationJpaRepository extends JpaRepository<TokenRevocationJpaEntity, String> {

    /**
     * Fetches all entries whose original token has not yet expired.
     * Used on startup to warm up the Redis blacklist from persistent state.
     */
    List<TokenRevocationJpaEntity> findAllByExpiresAtAfter(Instant now);

    /**
     * Deletes rows for tokens that have already expired naturally.
     * Called by the scheduled cleanup job every hour.
     */
    @Modifying
    @Query("DELETE FROM TokenRevocationJpaEntity t WHERE t.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") Instant now);
}