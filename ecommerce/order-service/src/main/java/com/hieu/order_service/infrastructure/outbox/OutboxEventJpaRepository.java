package com.hieu.order_service.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, String> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE processed_at IS NULL AND next_attempt_at <= now()
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockBatch(@Param("limit") int limit);

    @Modifying
    @Query("UPDATE OutboxEventJpaEntity o SET o.processedAt = :processedAt WHERE o.id = :id")
    void markProcessed(@Param("id") String id, @Param("processedAt") Instant processedAt);

    @Modifying
    @Query("UPDATE OutboxEventJpaEntity o SET o.retryCount = :retryCount, o.nextAttemptAt = :nextAttemptAt WHERE o.id = :id")
    void bumpRetry(@Param("id") String id, @Param("retryCount") int retryCount, @Param("nextAttemptAt") Instant nextAttemptAt);
}
