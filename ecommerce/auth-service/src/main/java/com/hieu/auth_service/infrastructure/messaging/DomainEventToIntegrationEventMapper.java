package com.hieu.auth_service.infrastructure.messaging;

import com.hieu.auth_service.application.events.IntegrationEvent;
import com.hieu.auth_service.application.events.KafkaTopics;
import com.hieu.auth_service.application.events.SessionIntegrationEvent;
import com.hieu.auth_service.application.events.UserLifecycleIntegrationEvent;
import com.hieu.auth_service.domain.events.DomainEvent;
import com.hieu.auth_service.domain.models.refreshtoken.events.TokenCreatedEvent;
import com.hieu.auth_service.domain.models.refreshtoken.events.TokenRevokedEvent;
import com.hieu.auth_service.domain.models.refreshtoken.events.TokenRotatedEvent;
import com.hieu.auth_service.domain.models.role.events.PermissionGrantedEvent;
import com.hieu.auth_service.domain.models.role.events.PermissionRevokedEvent;
import com.hieu.auth_service.domain.models.user.events.AccountStatusChangedEvent;
import com.hieu.auth_service.domain.models.user.events.EmailChangedEvent;
import com.hieu.auth_service.domain.models.user.events.PasswordChangedEvent;
import com.hieu.auth_service.domain.models.user.events.RoleAssignedEvent;
import com.hieu.auth_service.domain.models.user.events.RoleRemovedEvent;
import com.hieu.auth_service.domain.models.user.events.UserCreatedEvent;
import com.hieu.auth_service.domain.models.user.events.UserLoggedInEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts in-process {@link DomainEvent}s to the flat
 * {@link IntegrationEvent} payloads published to Kafka.
 *
 * <p>Split into its own class so the publisher stays a thin I/O shell and the mapping
 * rules are unit-testable in isolation. Pattern matching on the domain-event type keeps
 * the switch exhaustive at compile time.
 */
@Component
public class DomainEventToIntegrationEventMapper {

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_FAMILY = "family";
    private static final String FIELD_USER_ID = "userId";


    private static final int SCHEMA_V1 = 1;

    /** Routed payload ready for Kafka: {@code topic} + {@code key} + {@code value}. */
    public record Routed(String topic, String key, IntegrationEvent event) {}

    /**
     * Maps a domain event to a routed integration event.
     *
     * @param event raw domain event from an aggregate
     * @return {@code Routed} descriptor, or {@code null} if the event has no external
     *         significance (we don't leak every domain event to Kafka by default)
     */
    public Routed map(DomainEvent event) {
        return switch (event) {
            // ── User lifecycle → auth.user.events.v1 ───────────────────────────
            case UserCreatedEvent e -> lifecycle(
                    e, "auth.user.created.v1",
                    Map.of(FIELD_USERNAME, e.username(), "email", e.email()));

            case EmailChangedEvent e -> lifecycle(
                    e, "auth.user.email_changed.v1",
                    Map.of("oldEmail", e.oldEmail(), "newEmail", e.newEmail()));

            case PasswordChangedEvent e -> lifecycle(
                    e, "auth.user.password_changed.v1",
                    Map.of(FIELD_USERNAME, e.username()));

            case AccountStatusChangedEvent e -> lifecycle(
                    e, "auth.user.status_changed.v1",
                    Map.of(FIELD_USERNAME, e.username(), "transition", e.transition().name()));

            case RoleAssignedEvent e -> lifecycle(
                    e, "auth.user.role_assigned.v1",
                    Map.of("roleId", e.roleId()));

            case RoleRemovedEvent e -> lifecycle(
                    e, "auth.user.role_removed.v1",
                    Map.of("roleId", e.roleId()));

            // ── Session + tokens → auth.session.events.v1 ──────────────────────
            case UserLoggedInEvent e -> session(
                    e, "auth.user.logged_in.v1",
                    Map.of(FIELD_USERNAME, e.username()));

            case TokenCreatedEvent e -> session(
                    e, "auth.token.created.v1",
                    fields(FIELD_USER_ID, e.userId(), FIELD_FAMILY, e.family(), "generation", e.generation()));

            case TokenRotatedEvent e -> session(
                    e, "auth.token.rotated.v1",
                    fields(FIELD_USER_ID, e.userId(), FIELD_FAMILY, e.family(),
                            "oldTokenId", e.oldTokenId(), "newGeneration", e.newGeneration()));

            case TokenRevokedEvent e -> session(
                    e, "auth.token.revoked.v1",
                    fields(FIELD_USER_ID, e.userId(), FIELD_FAMILY, e.family(), "reason", e.reason()));

            // ── Role aggregate events — propagated for cache invalidation downstream ──
            case PermissionGrantedEvent e -> lifecycle(
                    e, "auth.role.permission_granted.v1",
                    Map.of("permissionId", e.permissionId()));

            case PermissionRevokedEvent e -> lifecycle(
                    e, "auth.role.permission_revoked.v1",
                    Map.of("permissionId", e.permissionId()));

            // Unknown / internal-only events: don't leak.
            default -> null;
        };
    }

    private Routed lifecycle(DomainEvent src, String type, Map<String, Object> payload) {
        return new Routed(
                KafkaTopics.AUTH_USER_EVENTS,
                src.aggregateId(),
                new UserLifecycleIntegrationEvent(
                        src.eventId(), type, src.occurredOn(), src.aggregateId(),
                        SCHEMA_V1, payload));
    }

    private Routed session(DomainEvent src, String type, Map<String, Object> payload) {
        return new Routed(
                KafkaTopics.AUTH_SESSION_EVENTS,
                src.aggregateId(),
                new SessionIntegrationEvent(
                        src.eventId(), type, src.occurredOn(), src.aggregateId(),
                        SCHEMA_V1, payload));
    }

    /** Flexible field-map builder that tolerates nullable values. */
    private static Map<String, Object> fields(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object val = i + 1 < keyValuePairs.length ? keyValuePairs[i + 1] : null;
            if (val != null) map.put(key, val);
        }
        return map;
    }
}
