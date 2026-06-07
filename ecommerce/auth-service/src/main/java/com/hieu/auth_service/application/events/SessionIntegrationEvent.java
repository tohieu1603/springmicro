package com.hieu.auth_service.application.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Session-related integration event (UserLoggedIn, TokenRotated, TokenRevoked).
 *
 * <p>Separate topic from user-lifecycle events because consumption patterns differ:
 * security / audit consumers want sessions, billing / profile consumers want lifecycle.
 */
public record SessionIntegrationEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        String aggregateId,
        int schemaVersion,
        Map<String, Object> payload
) implements IntegrationEvent {
}
