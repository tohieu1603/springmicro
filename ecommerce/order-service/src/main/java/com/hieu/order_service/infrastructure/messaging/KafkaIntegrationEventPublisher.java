package com.hieu.order_service.infrastructure.messaging;

import com.hieu.order_service.domain.events.DomainEvent;
import com.hieu.order_service.infrastructure.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges in-process domain events to the transactional outbox.
 * Runs BEFORE_COMMIT so the outbox INSERT is part of the same DB transaction
 * as the aggregate save — guarantees at-least-once delivery via the OutboxPoller.
 */
@Component
@RequiredArgsConstructor
public class KafkaIntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaIntegrationEventPublisher.class);

    private final OutboxWriter outboxWriter;
    private final DomainEventToIntegrationMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        var routed = mapper.map(event);
        if (routed == null) return;
        // Derive aggregate type from the simple class name of the aggregate (strip "Event" suffix context)
        String aggregateType = deriveAggregateType(event);
        outboxWriter.append(aggregateType, routed.key(), event.eventType(), routed.topic(), routed.payload());
        log.debug("Outbox appended {} aggregateType={} aggregateId={} topic={}",
                event.eventType(), aggregateType, routed.key(), routed.topic());
    }

    private String deriveAggregateType(DomainEvent event) {
        // Package path determines aggregate: order vs returnrequest
        String pkg = event.getClass().getPackageName();
        if (pkg.contains("returnrequest")) return "ReturnRequest";
        return "Order";
    }
}
