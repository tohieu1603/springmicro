package com.hieu.auth_service.application.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Flat, schema-versioned payload broadcast to external services for any user-lifecycle
 * transition (registration, email change, password change, role assignment, …).
 *
 * <p>One record with a {@code type} discriminator keeps the consumer side simple — a
 * single Kafka topic + a switch on {@code type}. The {@code payload} map carries
 * transition-specific fields (e.g. {@code oldEmail}/{@code newEmail}) so new
 * transitions can ship without schema churn.
 *
 * @param eventId       unique id per instance (derived from source DomainEvent)
 * @param eventType     discriminator ({@code auth.user.created.v1}, {@code auth.user.password_changed.v1}, ...)
 * @param occurredOn    wall-clock moment the source domain event fired
 * @param aggregateId   user id the event pertains to
 * @param schemaVersion bump on breaking payload change
 * @param payload       event-type-specific data
 */
public record UserLifecycleIntegrationEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        String aggregateId,
        int schemaVersion,
        Map<String, Object> payload
) implements IntegrationEvent {
}
