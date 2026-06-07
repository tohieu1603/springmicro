package com.hieu.catalog_service.application.common;

import com.hieu.catalog_service.domain.events.DomainEvent;
import com.hieu.catalog_service.domain.shared.AggregateRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bridges domain events to Spring's event bus. Handlers call
 * {@link #publishEventsOf(AggregateRoot)} AFTER a successful save; a downstream
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} then forwards them to Kafka
 * as integration events.
 *
 * <p>Publishing inside the surrounding transaction (but consuming AFTER_COMMIT) means
 * either both the DB row AND the Kafka publication happen, or neither — we never emit
 * a "product created" event for a product the DB rolled back.
 */
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishEventsOf(AggregateRoot aggregate) {
        List<DomainEvent> events = aggregate.pullDomainEvents();
        for (DomainEvent event : events) {
            publisher.publishEvent(event);
        }
    }
}
