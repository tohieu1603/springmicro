package com.hieu.auth_service.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.TokenRevocationJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.TokenRevocationJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hybrid Redis + DB blacklist for access tokens — implements {@link TokenBlacklistPort}.
 *
 * <p>Revoke flow:
 * <ol>
 *   <li>Redis {@code SET blacklist:{jti} = reason} with TTL = remaining lifetime
 *       (best-effort; failure is non-fatal).</li>
 *   <li>Always persist to {@code token_revocations} table — the source of truth.</li>
 * </ol>
 *
 * <p>Check flow: Redis-first (fast-path), falls back to DB on Redis outage.
 *
 * <p>On startup the service warms Redis from DB so a fresh Redis instance cannot silently
 * bypass active revocations persisted before the restart. A scheduled hourly job purges
 * rows whose JWT has expired naturally.
 *
 * <p>{@code userId} is stored as String (UUID) — previous {@code Long} typing was
 * incompatible with the domain model.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService implements TokenBlacklistPort {

    /** Redis key prefix. */
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final TokenRevocationJpaRepository tokenRevocationRepository;

    /**
     * Revokes an access token by its jti claim. Idempotent — re-revoking an already-revoked
     * token simply refreshes the Redis TTL and writes a new DB row iff one doesn't exist.
     *
     * @param tokenId   JWT jti
     * @param userId    owning user UUID (String)
     * @param expiresAt original JWT expiry instant
     * @param reason    free-form reason code (LOGOUT | ADMIN_REVOKE | SECURITY_BREACH | PASSWORD_CHANGED)
     */
    @Override
    @Transactional
    public void revoke(String tokenId, String userId, Instant expiresAt, String reason) {
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds > 0) {
            try {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + tokenId, reason, Duration.ofSeconds(ttlSeconds));
            } catch (Exception e) {
                log.warn("Redis unavailable for blacklist write; DB-only fallback: {}", e.getMessage());
            }
        }
        tokenRevocationRepository.save(
                TokenRevocationJpaEntity.forRevocation(tokenId, userId, expiresAt, reason));
    }

    /** Returns true when the given jti is in the blacklist (Redis fast-path, DB fallback). */
    @Override
    public boolean isRevoked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId));
        } catch (Exception e) {
            log.warn("Redis unavailable for blacklist check; falling back to DB: {}", e.getMessage());
            return tokenRevocationRepository.existsById(tokenId);
        }
    }

    /** Rebuilds Redis from DB on startup so a fresh Redis cannot silently bypass active revocations. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpRedisFromDb() {
        try {
            List<TokenRevocationJpaEntity> active =
                    tokenRevocationRepository.findAllByExpiresAtAfter(Instant.now());
            for (TokenRevocationJpaEntity row : active) {
                long ttl = Duration.between(Instant.now(), row.getExpiresAt()).getSeconds();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(
                            BLACKLIST_PREFIX + row.getId(),
                            row.getReason(),
                            Duration.ofSeconds(ttl));
                }
            }
            log.info("Blacklist warm-up: restored {} active revocation(s) into Redis", active.size());
        } catch (Exception e) {
            log.warn("Blacklist warm-up skipped: {}", e.getMessage());
        }
    }

    /** Hourly purge of rows whose original JWT has expired naturally. */
    @Scheduled(fixedRate = 3_600_000L)
    @Transactional
    public void cleanupExpiredRevocations() {
        tokenRevocationRepository.deleteByExpiresAtBefore(Instant.now());
        log.debug("Purged expired token revocation rows");
    }
}
