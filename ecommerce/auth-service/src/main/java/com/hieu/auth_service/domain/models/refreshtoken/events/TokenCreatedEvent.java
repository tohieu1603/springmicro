package com.hieu.auth_service.domain.models.refreshtoken.events;

import com.hieu.auth_service.domain.events.DomainEvent;

import java.time.Instant;

public final class TokenCreatedEvent extends DomainEvent {
    private final String tokenId;
    private final String userId;
    private final String family;
    private final int generation;
    private final Instant expiresAt;

    public TokenCreatedEvent(String tokenId, String userId, String family, int generation, Instant expiresAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.family = family;
        this.generation = generation;
        this.expiresAt = expiresAt;
    }

    @Override public String aggregateId() { return tokenId; }

    public String tokenId()   { return tokenId; }
    public String userId()    { return userId; }
    public String family()    { return family; }
    public int generation()   { return generation; }
    public Instant expiresAt(){ return expiresAt; }
}
