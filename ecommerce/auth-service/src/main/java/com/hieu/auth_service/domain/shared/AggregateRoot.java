package com.hieu.auth_service.domain.shared;

import com.hieu.auth_service.domain.events.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all aggregate roots.
 *
 * <p>Centralises domain-event bookkeeping so individual aggregates can focus on
 * business invariants. Infrastructure drains events via {@link #pullDomainEvents()}
 * after a successful transaction commit — a single drain semantics avoids the
 * "forgot to clearEvents" bug that plagues hand-rolled ArrayLists.
 */
public abstract class AggregateRoot {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    /** Aggregates call this from mutating methods when an invariant changes. */
    protected final void registerEvent(DomainEvent event) {
        if (event == null) return;
        this.domainEvents.add(event);
    }

    /**
     * Atomically returns a snapshot of pending events and clears the internal buffer.
     * Intended to be called exactly once per unit-of-work, typically inside repository
     * save() <em>after</em> persistence succeeds.
     */
    public final List<DomainEvent> pullDomainEvents() {
        if (domainEvents.isEmpty()) return List.of();
        List<DomainEvent> snapshot = List.copyOf(domainEvents);
        domainEvents.clear();
        return snapshot;
    }

    /** Non-destructive peek — for tests/logging only. */
    public final List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** Discards buffered events without publishing. Use sparingly (e.g. rollback handling). */
    protected final void clearDomainEvents() {
        domainEvents.clear();
    }
}
