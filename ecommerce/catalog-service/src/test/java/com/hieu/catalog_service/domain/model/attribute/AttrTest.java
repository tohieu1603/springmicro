package com.hieu.catalog_service.domain.model.attribute;

import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Attr aggregate (unit)")
class AttrTest {

    @Test
    @DisplayName("create() upper-cases the code and trims the name")
    void create() {
        Attr a = Attr.create("color", "  Color  ", AttrType.SELECT);
        assertThat(a.getCode()).isEqualTo("COLOR");
        assertThat(a.getName()).isEqualTo("Color");
        assertThat(a.getType()).isEqualTo(AttrType.SELECT);
    }

    @Test
    @DisplayName("create() rejects a blank code or name")
    void createBlank() {
        assertThatThrownBy(() -> Attr.create(" ", "Color", AttrType.SELECT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attr.create("color", " ", AttrType.SELECT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SELECT attrs accept predefined values; duplicates by code are rejected")
    void addValue() {
        Attr a = Attr.create("color", "Color", AttrType.SELECT);
        a.addValue(AttrVal.create("attr-1", "Red", "RED"));

        assertThat(a.getValues()).hasSize(1);
        assertThatThrownBy(() -> a.addValue(AttrVal.create("attr-1", "Reddish", "RED")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("non-SELECT attrs reject predefined values")
    void addValueOnTextRejected() {
        Attr text = Attr.create("note", "Note", AttrType.TEXT);
        assertThatThrownBy(() -> text.addValue(AttrVal.create("attr-1", "x", "X")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("removeValue removes a predefined value by id")
    void removeValue() {
        Attr a = Attr.create("color", "Color", AttrType.SELECT);
        AttrVal red = AttrVal.create("attr-1", "Red", "RED");
        red.assignId("10");
        a.addValue(red);

        a.removeValue("10");

        assertThat(a.getValues()).isEmpty();
    }

    @Test
    @DisplayName("allowsFreeText reflects the type")
    void allowsFreeText() {
        assertThat(Attr.create("note", "Note", AttrType.TEXT).allowsFreeText()).isTrue();
        assertThat(Attr.create("weight", "Weight", AttrType.NUMBER).allowsFreeText()).isTrue();
        assertThat(Attr.create("color", "Color", AttrType.SELECT).allowsFreeText()).isFalse();
    }
}
