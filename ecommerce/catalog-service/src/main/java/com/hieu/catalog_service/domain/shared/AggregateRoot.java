package com.hieu.catalog_service.domain.shared;

import com.hieu.catalog_service.domain.events.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots — centralises domain-event bookkeeping.
 *
 * <p>Aggregates call {@link #registerEvent(DomainEvent)} from mutating methods.
 * Infrastructure drains the buffer via {@link #pullDomainEvents()} after a
 * successful transactional commit — single-drain semantics avoid the
 * "forgot to clearEvents" bug that plagues hand-rolled lists.
 */
public abstract class AggregateRoot {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected final void registerEvent(DomainEvent event) {
        if (event != null) this.domainEvents.add(event);
    }

    /**
     * Atomically returns a snapshot of pending events and clears the buffer.
     * Invoked by repository adapters right after {@code jpaRepository.save()} succeeds.
     */
    public final List<DomainEvent> pullDomainEvents() {
        if (domainEvents.isEmpty()) return List.of();
        List<DomainEvent> snapshot = List.copyOf(domainEvents);
        domainEvents.clear();
        return snapshot;
    }

    /** Non-destructive peek — for tests / logging only. */
    public final List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** Drops buffered events without publishing. Reserved for rollback flows. */
    protected final void clearDomainEvents() {
        domainEvents.clear();
    }
}
