package com.hieu.auth_service.domain.models.refreshtoken.events;

import com.hieu.auth_service.domain.events.DomainEvent;

/**
 * Emitted when a refresh token is successfully rotated — the old token id is revoked
 * and a new one is issued in the same family with generation = old + 1.
 */
public final class TokenRotatedEvent extends DomainEvent {
    private final String userId;
    private final String family;
    private final String oldTokenId;
    private final String newTokenId;
    private final int newGeneration;

    public TokenRotatedEvent(String userId, String family,
                             String oldTokenId, String newTokenId, int newGeneration) {
        this.userId = userId;
        this.family = family;
        this.oldTokenId = oldTokenId;
        this.newTokenId = newTokenId;
        this.newGeneration = newGeneration;
    }

    @Override public String aggregateId() { return newTokenId; }

    public String userId()      { return userId; }
    public String family()      { return family; }
    public String oldTokenId()  { return oldTokenId; }
    public String newTokenId()  { return newTokenId; }
    public int newGeneration()  { return newGeneration; }
}
