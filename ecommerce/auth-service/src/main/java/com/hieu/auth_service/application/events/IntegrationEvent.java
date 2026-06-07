package com.hieu.auth_service.application.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract for cross-service events published to Kafka.
 *
 * <p>Distinct from {@link com.hieu.auth_service.domain.events.DomainEvent} (which
 * lives in-process): integration events cross the bounded-context boundary, so they
 * are purposefully flat, versioned, and stable.
 *
 * <p>Producers implement as records and derive {@code eventId}/{@code occurredOn}
 * from the source {@code DomainEvent} so duplicate delivery stays idempotent
 * downstream (consumers can dedupe on {@code eventId}).
 */
public interface IntegrationEvent {

    /** Unique id per event instance, stable across retries. */
    UUID eventId();

    /** Machine-readable event type, e.g. {@code auth.user.created.v1}. */
    String eventType();

    /** Moment the originating domain event was raised. */
    Instant occurredOn();

    /** Aggregate this event pertains to (typically the user id). */
    String aggregateId();

    /** Payload schema version — bump on breaking changes. */
    int schemaVersion();
}
