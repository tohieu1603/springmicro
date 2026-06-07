package com.hieu.catalog_service.infrastructure.messaging;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges in-process {@link DomainEvent}s to Kafka AFTER_COMMIT. Failures are logged
 * but not retried — upgrade to the Transactional Outbox pattern if at-least-once
 * delivery becomes a hard requirement.
 */
@Component
@RequiredArgsConstructor
public class KafkaIntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaIntegrationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DomainEventToIntegrationMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        var routed = mapper.map(event);
        if (routed == null) return;
        try {
            kafkaTemplate.send(routed.topic(), routed.key(), routed.payload());
            log.debug("Published {} to {} (key={})", event.eventType(), routed.topic(), routed.key());
        } catch (Exception e) {
            log.warn("Failed to publish {} to {}: {}", event.eventType(), routed.topic(), e.getMessage());
        }
    }
}
