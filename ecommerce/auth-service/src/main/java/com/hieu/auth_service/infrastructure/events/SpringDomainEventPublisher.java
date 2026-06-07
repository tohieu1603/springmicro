package com.hieu.auth_service.infrastructure.events;

import com.hieu.auth_service.domain.events.DomainEvent;
import com.hieu.auth_service.domain.events.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring {@link ApplicationEventPublisher}-backed implementation of the domain port.
 *
 * <p>Logs event type + id (not payload — may contain PII) so ops can correlate across
 * aggregates without leaking user data into log aggregators.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final ApplicationEventPublisher delegate;

    public SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) return;
        log.debug("Publishing {} [eventId={}, aggregateId={}]",
                event.eventType(), event.eventId(), event.aggregateId());
        delegate.publishEvent(event);
    }
}
