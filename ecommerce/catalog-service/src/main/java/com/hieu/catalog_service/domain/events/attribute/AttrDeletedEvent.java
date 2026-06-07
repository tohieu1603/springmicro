package com.hieu.catalog_service.domain.events.attribute;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class AttrDeletedEvent extends DomainEvent {

    private final String attrId;

    public AttrDeletedEvent(String attrId) {
        this.attrId = Objects.requireNonNull(attrId, "attrId");
    }

    @Override public String aggregateId() { return attrId; }
}
