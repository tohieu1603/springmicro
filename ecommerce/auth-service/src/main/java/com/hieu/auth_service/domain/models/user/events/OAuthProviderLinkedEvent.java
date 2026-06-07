package com.hieu.auth_service.domain.models.user.events;

import com.hieu.auth_service.domain.events.DomainEvent;

/**
 * Raised when a user account is linked with an OAuth provider (Google for now).
 * Either on a brand-new social registration or on backfill for an existing
 * password account that signs in via Google for the first time.
 */
public final class OAuthProviderLinkedEvent extends DomainEvent {
    private final String userId;
    private final String provider;        // "google"
    private final String providerUserId;  // Google sub
    private final boolean newAccount;

    public OAuthProviderLinkedEvent(String userId, String provider,
                                    String providerUserId, boolean newAccount) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.newAccount = newAccount;
    }

    @Override public String aggregateId() { return userId; }

    public String userId()         { return userId; }
    public String provider()       { return provider; }
    public String providerUserId() { return providerUserId; }
    public boolean newAccount()    { return newAccount; }
}
