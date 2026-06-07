package com.hieu.auth_service.domain.models.refreshtoken.events;

import com.hieu.auth_service.domain.events.DomainEvent;

public final class TokenRevokedEvent extends DomainEvent {
    private final String tokenId;
    private final String userId;
    private final String family;
    private final String reason;

    public TokenRevokedEvent(String tokenId, String userId, String family, String reason) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.family = family;
        this.reason = reason;
    }

    @Override public String aggregateId() { return tokenId; }

    public String tokenId() { return tokenId; }
    public String userId()  { return userId; }
    public String family()  { return family; }
    public String reason()  { return reason; }
}
