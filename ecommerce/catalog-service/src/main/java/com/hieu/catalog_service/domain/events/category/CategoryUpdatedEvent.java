package com.hieu.catalog_service.domain.events.category;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class CategoryUpdatedEvent extends DomainEvent {

    private final String categoryId;
    private final String name;
    private final String parentId;
    private final String updatedBy;

    public CategoryUpdatedEvent(String categoryId, String name, String parentId, String updatedBy) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.name = Objects.requireNonNull(name, "name");
        this.parentId = parentId;
        this.updatedBy = updatedBy;
    }

    @Override public String aggregateId() { return categoryId; }
}
