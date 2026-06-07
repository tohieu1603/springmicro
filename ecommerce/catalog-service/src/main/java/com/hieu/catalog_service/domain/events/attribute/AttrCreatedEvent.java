package com.hieu.catalog_service.domain.events.attribute;

import com.hieu.catalog_service.domain.events.DomainEvent;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class AttrCreatedEvent extends DomainEvent {

    private final String attrId;
    private final String code;
    private final String name;
    private final AttrType type;

    public AttrCreatedEvent(String attrId, String code, String name, AttrType type) {
        this.attrId = Objects.requireNonNull(attrId, "attrId");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override public String aggregateId() { return attrId; }
}
