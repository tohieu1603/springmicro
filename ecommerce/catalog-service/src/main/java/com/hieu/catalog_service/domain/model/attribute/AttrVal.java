package com.hieu.catalog_service.domain.model.attribute;

import lombok.Getter;

import java.util.Objects;

/** Predefined value for a SELECT attribute (e.g. Color=Red). Part of the {@link Attr} aggregate. */
@Getter
public final class AttrVal {

    private String id;
    private final String attrId;
    private String val;
    private String code;
    private int sortOrder;

    private AttrVal(String id, String attrId, String val, String code, int sortOrder) {
        this.id = id;
        this.attrId = attrId;
        this.val = Objects.requireNonNull(val, "val");
        this.code = Objects.requireNonNull(code, "code");
        this.sortOrder = sortOrder;
    }

    public static AttrVal create(String attrId, String val, String code) {
        Objects.requireNonNull(val, "val");
        String resolvedCode = (code != null && !code.isBlank())
            ? code.trim().toUpperCase()
            : val.trim().toUpperCase().replaceAll("\\s+", "_");
        return new AttrVal(null, attrId, val.trim(), resolvedCode, 0);
    }

    public static AttrVal reconstitute(String id, String attrId, String val, String code, int sortOrder) {
        Objects.requireNonNull(id, "id");
        return new AttrVal(id, attrId, val, code, sortOrder);
    }

    public void assignId(String id) {
        if (this.id != null) throw new IllegalStateException("AttrVal id already set");
        this.id = Objects.requireNonNull(id, "id");
    }

    public void update(String val, String code, Integer sortOrder) {
        if (val != null) this.val = val.trim();
        if (code != null) this.code = code.trim().toUpperCase();
        if (sortOrder != null) this.sortOrder = sortOrder;
    }
}
