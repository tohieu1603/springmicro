package com.hieu.auth_service.domain.models.user.events;

import com.hieu.auth_service.domain.events.DomainEvent;

public final class UserCreatedEvent extends DomainEvent {
    private final String userId;
    private final String username;
    private final String email;

    public UserCreatedEvent(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    @Override public String aggregateId() { return userId; }

    public String userId()   { return userId; }
    public String username() { return username; }
    public String email()    { return email; }
}
