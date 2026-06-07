package com.hieu.catalog_service.domain.model.category;

import com.hieu.catalog_service.domain.events.category.CategoryCreatedEvent;
import com.hieu.catalog_service.domain.events.category.CategoryDeletedEvent;
import com.hieu.catalog_service.domain.events.category.CategoryUpdatedEvent;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryDescription;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;
import com.hieu.catalog_service.domain.shared.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Category aggregate root. Adjacency model ({@code parentId}) keeps parent lookups cheap;
 * migrate to nested-set / materialised-path only if deep-tree queries become hot.
 */
@Getter
public final class Category extends AggregateRoot {

    private CategoryId id;
    private CategoryName name;
    private CategoryDescription description;
    private CategoryId parentId;
    private boolean active;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    private final String createdBy;
    private String updatedBy;

    private Category(CategoryId id, CategoryName name, CategoryDescription description, CategoryId parentId,
                     boolean active, int sortOrder, Instant createdAt, Instant updatedAt,
                     String createdBy, String updatedBy) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.parentId = parentId;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public static Category create(CategoryName name, CategoryDescription description, CategoryId parentId,
                                   int sortOrder, String createdBy) {
        Instant now = Instant.now();
        return new Category(null, name, description, parentId, true, sortOrder, now, now, createdBy, createdBy);
    }

    public static Category reconstitute(CategoryId id, CategoryName name, CategoryDescription description,
                                         CategoryId parentId, boolean active, int sortOrder,
                                         Instant createdAt, Instant updatedAt,
                                         String createdBy, String updatedBy) {
        Objects.requireNonNull(id, "id");
        return new Category(id, name, description, parentId, active, sortOrder,
                            createdAt, updatedAt, createdBy, updatedBy);
    }

    public void assignId(CategoryId id) {
        if (this.id != null) throw new IllegalStateException("Category id already set: " + this.id);
        this.id = Objects.requireNonNull(id, "id");
    }

    public void raiseCreatedEvent() {
        if (id == null) throw new IllegalStateException("Category not persisted yet");
        registerEvent(new CategoryCreatedEvent(
            id.value(), name.value(),
            parentId != null ? parentId.value() : null, createdBy));
    }

    public void update(CategoryName name, CategoryDescription description, CategoryId parentId,
                       int sortOrder, String updatedBy) {
        if (parentId != null && this.id != null && parentId.equals(this.id)) {
            throw new IllegalArgumentException("Category cannot be its own parent");
        }
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        touch(updatedBy);
        if (id != null) registerEvent(new CategoryUpdatedEvent(
            id.value(), name.value(),
            parentId != null ? parentId.value() : null, updatedBy));
    }

    public void activate(String updatedBy)   { if (!active) { active = true; touch(updatedBy); } }
    public void deactivate(String updatedBy) { if (active) { active = false; touch(updatedBy); } }

    public void softDelete(String deletedBy) {
        if (!active && id != null) return;
        active = false;
        touch(deletedBy);
        if (id != null) registerEvent(new CategoryDeletedEvent(id.value(), deletedBy));
    }

    public boolean isActive() { return active; }

    private void touch(String by) {
        this.updatedBy = by;
        this.updatedAt = Instant.now();
    }
}
