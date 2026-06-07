package com.hieu.catalog_service.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for in-process domain events raised by catalog aggregates.
 *
 * <p>Immutable by convention. Subclasses should be final records or classes with
 * only {@code final} fields. {@code eventId} / {@code occurredOn} are auto-generated
 * to guarantee unique + monotonic ordering without bugs in subclasses.
 */
public abstract class DomainEvent {

    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredOn = Instant.now();

    public final UUID eventId()      { return eventId; }
    public final Instant occurredOn(){ return occurredOn; }

    /** Simple class name — cheap + stable across redeploys of same class. */
    public String eventType() { return getClass().getSimpleName(); }

    /** Aggregate id the event pertains to — used as Kafka partition key. */
    public abstract String aggregateId();

    @Override
    public String toString() {
        return "%s{eventId=%s, aggregateId=%s, occurredOn=%s}"
                .formatted(eventType(), eventId, aggregateId(), occurredOn);
    }
}
