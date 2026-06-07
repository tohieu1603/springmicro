package com.hieu.catalog_service.domain.events.attribute;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class AttrUpdatedEvent extends DomainEvent {

    private final String attrId;
    private final String name;

    public AttrUpdatedEvent(String attrId, String name) {
        this.attrId = Objects.requireNonNull(attrId, "attrId");
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override public String aggregateId() { return attrId; }
}
