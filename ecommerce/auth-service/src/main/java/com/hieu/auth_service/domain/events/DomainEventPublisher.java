package com.hieu.auth_service.domain.events;

/**
 * Outbound port — domain publishes events without knowing about any broker/framework.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);

    /** Accepts any iterable of events (or subtype). Implementations may batch. */
    default void publishAll(Iterable<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
