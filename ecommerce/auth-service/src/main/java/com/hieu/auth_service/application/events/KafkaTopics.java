package com.hieu.auth_service.application.events;

/**
 * Single source of truth for Kafka topic names emitted by auth-service.
 *
 * <p>Versioned suffix ({@code .v1}) so consumers can reason about schema stability:
 * a breaking payload change ships on a new topic (v2) side-by-side until migration.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    /** Lifecycle events: UserCreated, PasswordChanged, EmailChanged, RoleAssigned/Removed, AccountStatusChanged. */
    public static final String AUTH_USER_EVENTS = "auth.user.events.v1";

    /** Session lifecycle events: UserLoggedIn, TokenRevoked, TokenRotated. */
    public static final String AUTH_SESSION_EVENTS = "auth.session.events.v1";
}
