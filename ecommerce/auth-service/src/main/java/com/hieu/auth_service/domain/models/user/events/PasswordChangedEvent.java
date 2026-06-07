package com.hieu.auth_service.domain.models.user.events;

import com.hieu.auth_service.domain.events.DomainEvent;

public final class PasswordChangedEvent extends DomainEvent {
    private final String userId;
    private final String username;

    public PasswordChangedEvent(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    @Override public String aggregateId() { return userId; }

    public String userId()   { return userId; }
    public String username() { return username; }
}
