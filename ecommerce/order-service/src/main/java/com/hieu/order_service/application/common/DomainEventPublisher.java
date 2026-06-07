package com.hieu.order_service.application.common;

import com.hieu.order_service.domain.events.DomainEvent;
import com.hieu.order_service.domain.shared.AggregateRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/** Bridges domain events to Spring's event bus. */
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishEventsOf(AggregateRoot aggregate) {
        List<DomainEvent> events = aggregate.pullDomainEvents();
        for (var event : events) {
            publisher.publishEvent(event);
        }
    }
}
