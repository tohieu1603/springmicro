package com.hieu.order_service.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Appends an outbox row within the caller's existing transaction.
 * Must be called from a BEFORE_COMMIT context so the INSERT commits
 * atomically with the domain aggregate save.
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private static final Logger log = LoggerFactory.getLogger(OutboxWriter.class);

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public void append(String aggregateType, String aggregateId, String eventType,
                       String topic, Object payload) {
        try {
            var entity = new OutboxEventJpaEntity();
            entity.setAggregateType(aggregateType);
            entity.setAggregateId(aggregateId);
            entity.setEventType(eventType);
            entity.setTopic(topic);
            entity.setPayload(objectMapper.writeValueAsString(payload));
            entity.setCreatedAt(Instant.now());
            entity.setRetryCount(0);
            entity.setNextAttemptAt(Instant.now());
            repository.save(entity);
        } catch (Exception e) {
            log.error("Failed to append outbox event [type={}, aggregateId={}]: {}",
                    eventType, aggregateId, e.getMessage(), e);
            throw new RuntimeException("Outbox append failed", e);
        }
    }
}
