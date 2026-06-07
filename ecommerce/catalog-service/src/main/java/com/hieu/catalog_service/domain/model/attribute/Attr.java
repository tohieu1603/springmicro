package com.hieu.catalog_service.domain.model.attribute;

import com.hieu.catalog_service.domain.events.attribute.AttrCreatedEvent;
import com.hieu.catalog_service.domain.events.attribute.AttrDeletedEvent;
import com.hieu.catalog_service.domain.events.attribute.AttrUpdatedEvent;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.shared.AggregateRoot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Attribute definition aggregate. SELECT attrs own a list of {@link AttrVal}s; TEXT/NUMBER
 * attrs store free-form values directly on variants. {@code code} is the stable
 * machine-readable key (upper-snake), {@code name} is the display label.
 */
@Getter
public final class Attr extends AggregateRoot {

    private AttrId id;
    private final String code;
    private String name;
    private AttrType type;
    private int sortOrder;

    private final List<AttrVal> values = new ArrayList<>();

    private Attr(AttrId id, String code, String name, AttrType type, int sortOrder) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.sortOrder = sortOrder;
    }

    public static Attr create(String code, String name, AttrType type) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        return new Attr(null, code.trim().toUpperCase(), name.trim(), type, 0);
    }

    public static Attr reconstitute(AttrId id, String code, String name, AttrType type, int sortOrder,
                                     List<AttrVal> values) {
        Objects.requireNonNull(id, "id");
        Attr a = new Attr(id, code, name, type, sortOrder);
        if (values != null) a.values.addAll(values);
        return a;
    }

    public void assignId(AttrId id) {
        if (this.id != null) throw new IllegalStateException("Attr id already set");
        this.id = Objects.requireNonNull(id, "id");
    }

    public void raiseCreatedEvent() {
        if (id == null) throw new IllegalStateException("Attr not persisted yet");
        registerEvent(new AttrCreatedEvent(id.value(), code, name, type));
    }

    public void update(String name, AttrType type, Integer sortOrder) {
        boolean changed = false;
        if (name != null && !name.equals(this.name))         { this.name = name.trim(); changed = true; }
        if (type != null && type != this.type)               { this.type = type; changed = true; }
        if (sortOrder != null && sortOrder != this.sortOrder){ this.sortOrder = sortOrder; changed = true; }
        if (changed && id != null) registerEvent(new AttrUpdatedEvent(id.value(), this.name));
    }

    public void addValue(AttrVal val) {
        if (type != AttrType.SELECT) throw new IllegalStateException("Only SELECT attrs have predefined values");
        Objects.requireNonNull(val, "val");
        if (values.stream().anyMatch(v -> v.getCode().equals(val.getCode()))) {
            throw new IllegalArgumentException("Duplicate value code: " + val.getCode());
        }
        values.add(val);
    }

    public void removeValue(String valId) {
        values.removeIf(v -> Objects.equals(v.getId(), valId));
    }

    public void markDeleted() {
        if (id != null) registerEvent(new AttrDeletedEvent(id.value()));
    }

    public boolean allowsFreeText() { return type.allowsFreeText(); }

    public List<AttrVal> getValues() { return Collections.unmodifiableList(values); }
}
