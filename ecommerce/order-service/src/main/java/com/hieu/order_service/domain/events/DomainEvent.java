package com.hieu.order_service.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for in-process domain events raised by order aggregates.
 *
 * <p>Note: cross-package {@code sealed permits} requires a module-info, which the
 * unnamed-module Spring Boot fat jar doesn't have. Kept as a non-sealed interface;
 * exhaustive pattern-matching in dispatchers is enforced via a {@code default}
 * branch that throws on unknown subtypes.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredOn();

    /** Aggregate id — used as Kafka partition key. */
    String aggregateId();

    default String eventType() { return getClass().getSimpleName(); }
}
