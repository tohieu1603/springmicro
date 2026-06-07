package com.hieu.catalog_service.domain.model.product;

import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import lombok.Getter;

import java.util.Objects;

/**
 * Link between a {@link Variant} and a single attribute/value pair — e.g. "Color = Red",
 * "Size = XL". Part of the {@link Product} aggregate; has no identity outside the parent
 * variant and never fires events on its own.
 *
 * <p>For {@link com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType#SELECT}
 * attributes, {@code valId} must resolve to a row in {@code attr_vals} and {@code valText}
 * is denormalised for fast reads. For {@code TEXT}/{@code NUMBER}, {@code valId} is
 * {@code null} and {@code valText} holds the free-text value.
 */
@Getter
public final class VariantAttr {

    private String id;                 // null until persisted
    private final AttrId attrId;
    private final String attrCode;
    private final String attrName;
    private final String valId;        // null for free-text
    private final String valText;

    private VariantAttr(String id, AttrId attrId, String attrCode, String attrName, String valId, String valText) {
        this.id = id;
        this.attrId = Objects.requireNonNull(attrId, "attrId");
        this.attrCode = Objects.requireNonNull(attrCode, "attrCode");
        this.attrName = Objects.requireNonNull(attrName, "attrName");
        this.valId = valId;
        this.valText = valText;
    }

    public static VariantAttr create(AttrId attrId, String attrCode, String attrName, String valId, String valText) {
        if (valId == null && (valText == null || valText.isBlank())) {
            throw new IllegalArgumentException(
                "VariantAttr must have either a valId (SELECT) or non-blank valText (TEXT/NUMBER)");
        }
        return new VariantAttr(null, attrId, attrCode, attrName, valId, valText);
    }

    public static VariantAttr reconstitute(String id, AttrId attrId, String attrCode, String attrName,
                                           String valId, String valText) {
        Objects.requireNonNull(id, "id");
        return new VariantAttr(id, attrId, attrCode, attrName, valId, valText);
    }

    public void assignId(String id) {
        if (this.id != null) throw new IllegalStateException("VariantAttr id is already set");
        this.id = Objects.requireNonNull(id, "id");
    }

    public String displayValue() {
        return valText != null ? valText : "";
    }
}
