package com.hieu.auth_service.domain.models.user.events;

import com.hieu.auth_service.domain.events.DomainEvent;

/**
 * Emitted when a user's account-status flag transitions (locked/unlocked/enabled/disabled).
 * One event class with a discriminator is friendlier for downstream consumers than
 * four nearly-identical classes.
 */
public final class AccountStatusChangedEvent extends DomainEvent {

    public enum Transition { LOCKED, UNLOCKED, DISABLED, ENABLED }

    private final String userId;
    private final String username;
    private final Transition transition;

    public AccountStatusChangedEvent(String userId, String username, Transition transition) {
        this.userId = userId;
        this.username = username;
        this.transition = transition;
    }

    @Override public String aggregateId() { return userId; }

    public String userId()         { return userId; }
    public String username()       { return username; }
    public Transition transition() { return transition; }

    @Override
    public String eventType() {
        // Keep discriminator in the event type for log/Kafka consumers that branch on name.
        return "Account" + transition.name().charAt(0)
                + transition.name().substring(1).toLowerCase() + "Event";
    }
}
