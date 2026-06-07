package com.hieu.catalog_service.infrastructure.persistence.mapper;

import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.AttrJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.AttrValJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for the hand-written Attr aggregate ↔ JPA mapper: scalar mapping both
 * directions, AttrType enum packing/unpacking, and the in-place child-value reconciliation
 * (insert / update-by-id / orphan removal) that keeps Hibernate's managed collection
 * identity stable.
 */
@DisplayName("AttrJpaMapper — unit")
class AttrJpaMapperTest {

    private final AttrJpaMapper mapper = new AttrJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("maps scalar fields and enum type to its name")
        void mapsScalars() {
            Attr a = Attr.reconstitute(AttrId.of("1"), "COLOR", "Color", AttrType.SELECT, 5, List.of());

            AttrJpaEntity e = mapper.toJpa(a, null);

            assertThat(e.getCode()).isEqualTo("COLOR");
            assertThat(e.getName()).isEqualTo("Color");
            assertThat(e.getType()).isEqualTo("SELECT");
            assertThat(e.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("new value (null id) becomes a fresh child wired to the attr")
        void insertsNewValue() {
            var val = AttrVal.create("attr-1", "Red", "RED");
            Attr a = Attr.reconstitute(AttrId.of("1"), "COLOR", "Color", AttrType.SELECT, 0, List.of(val));

            AttrJpaEntity e = mapper.toJpa(a, null);

            assertThat(e.getValues()).singleElement().satisfies(v -> {
                assertThat(v.getId()).isNull();
                assertThat(v.getVal()).isEqualTo("Red");
                assertThat(v.getCode()).isEqualTo("RED");
                assertThat(v.getAttr()).isSameAs(e);
            });
        }

        @Test
        @DisplayName("existing value id is matched and the same child instance mutated in place")
        void updatesExistingValueInPlace() {
            var existing = new AttrJpaEntity();
            existing.setId("1");
            var existingVal = new AttrValJpaEntity();
            existingVal.setId("50");
            existingVal.setVal("OldRed");
            existingVal.setCode("RED");
            existingVal.setAttr(existing);
            existing.getValues().add(existingVal);

            var domainVal = AttrVal.reconstitute("50", "1", "Crimson", "RED", 2);
            Attr a = Attr.reconstitute(AttrId.of("1"), "COLOR", "Color", AttrType.SELECT, 0, List.of(domainVal));

            AttrJpaEntity e = mapper.toJpa(a, existing);

            assertThat(e.getValues()).singleElement().satisfies(v -> {
                assertThat(v).isSameAs(existingVal);
                assertThat(v.getVal()).isEqualTo("Crimson");
                assertThat(v.getSortOrder()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("value absent from domain is dropped (orphan removal)")
        void removesMissingValue() {
            var existing = new AttrJpaEntity();
            existing.setId("1");
            var keep = new AttrValJpaEntity(); keep.setId("50"); keep.setVal("Red"); keep.setCode("RED"); keep.setAttr(existing);
            var drop = new AttrValJpaEntity(); drop.setId("51"); drop.setVal("Blue"); drop.setCode("BLUE"); drop.setAttr(existing);
            existing.getValues().add(keep);
            existing.getValues().add(drop);

            var domainVal = AttrVal.reconstitute("50", "1", "Red", "RED", 0);
            Attr a = Attr.reconstitute(AttrId.of("1"), "COLOR", "Color", AttrType.SELECT, 0, List.of(domainVal));

            AttrJpaEntity e = mapper.toJpa(a, existing);

            assertThat(e.getValues()).extracting(AttrValJpaEntity::getCode).containsExactly("RED");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("reconstitutes attr and its values, parsing the type string")
        void mapsAll() {
            var e = new AttrJpaEntity();
            e.setId("1");
            e.setCode("SIZE");
            e.setName("Size");
            e.setType("TEXT");
            e.setSortOrder(3);
            var val = new AttrValJpaEntity();
            val.setId("7");
            val.setVal("XL");
            val.setCode("XL");
            val.setSortOrder(1);
            val.setAttr(e);
            e.getValues().add(val);

            Attr a = mapper.toDomain(e);

            assertThat(a.getId().value()).isEqualTo("1");
            assertThat(a.getCode()).isEqualTo("SIZE");
            assertThat(a.getName()).isEqualTo("Size");
            assertThat(a.getType()).isEqualTo(AttrType.TEXT);
            assertThat(a.getSortOrder()).isEqualTo(3);
            assertThat(a.getValues()).singleElement().satisfies(v -> {
                assertThat(v.getId()).isEqualTo("7");
                assertThat(v.getVal()).isEqualTo("XL");
                assertThat(v.getCode()).isEqualTo("XL");
                assertThat(v.getAttrId()).isEqualTo("1");
            });
        }
    }

    @Test
    @DisplayName("round-trip toJpa → toDomain preserves attr and value fields")
    void roundTrip() {
        var val = AttrVal.reconstitute("7", "1", "Red", "RED", 0);
        Attr original = Attr.reconstitute(AttrId.of("1"), "COLOR", "Color", AttrType.SELECT, 2, List.of(val));

        AttrJpaEntity e = mapper.toJpa(original, null);
        e.setId("1");
        // toJpa builds a fresh child value with no id; reconstitute() requires non-null id,
        // so simulate the post-flush id assignment before reading back.
        e.getValues().get(0).setId("7");
        Attr back = mapper.toDomain(e);

        assertThat(back.getCode()).isEqualTo("COLOR");
        assertThat(back.getType()).isEqualTo(AttrType.SELECT);
        assertThat(back.getSortOrder()).isEqualTo(2);
        assertThat(back.getValues()).singleElement().satisfies(v ->
            assertThat(v.getVal()).isEqualTo("Red"));
    }
}
