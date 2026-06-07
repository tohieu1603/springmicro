package com.hieu.auth_service.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for in-process domain events raised by aggregates.
 *
 * <p>Subclasses should be immutable (final fields, no  ) and carry only the
 * context needed by downstream consumers — no framework types, no entity references.
 * {@code eventId}, {@code occurredOn}, and {@code eventType} are assigned automatically.
 */
public abstract class DomainEvent {

    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredOn = Instant.now();

    /** Unique id of this event instance. Stable across serialization. */
    public final UUID eventId() {
        return eventId;
    }

    /** Wall-clock instant the event was raised. */
    public final Instant occurredOn() {
        return occurredOn;
    }

    /** Simple-class-name — cheap, stable across redeploys of the same class. */
    public String eventType() {
        return getClass().getSimpleName();
    }

    /**
     * Id of the aggregate that raised this event. Subclasses override to expose
     * a domain-meaningful identifier for routing / partitioning.
     */
    public abstract String aggregateId();

    @Override
    public String toString() {
        return "%s{eventId=%s, aggregateId=%s, occurredOn=%s}"
                .formatted(eventType(), eventId, aggregateId(), occurredOn);
    }
}
