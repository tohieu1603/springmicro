package com.hieu.order_service.domain.shared;

import com.hieu.order_service.domain.events.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class for aggregate roots — centralises domain-event bookkeeping. */
public abstract class AggregateRoot {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected final void registerEvent(DomainEvent event) {
        if (event != null) domainEvents.add(event);
    }

    public final List<DomainEvent> pullDomainEvents() {
        if (domainEvents.isEmpty()) return List.of();
        var snapshot = List.copyOf(domainEvents);
        domainEvents.clear();
        return snapshot;
    }

    public final List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    protected final void clearDomainEvents() {
        domainEvents.clear();
    }
}
