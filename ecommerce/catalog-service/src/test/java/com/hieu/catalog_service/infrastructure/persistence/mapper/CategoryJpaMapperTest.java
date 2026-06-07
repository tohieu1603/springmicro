package com.hieu.catalog_service.infrastructure.persistence.mapper;

import com.hieu.catalog_service.domain.model.category.Category;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryDescription;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.CategoryJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for the hand-written Category ↔ JPA mapper: field mapping both
 * directions, null description / parentId packing/unpacking, and existing-entity reuse.
 */
@DisplayName("CategoryJpaMapper — unit")
class CategoryJpaMapperTest {

    private final CategoryJpaMapper mapper = new CategoryJpaMapper();

    private static Category category(String id, String parentId, String description) {
        Instant now = Instant.parse("2024-02-02T10:00:00Z");
        return Category.reconstitute(
            CategoryId.of(id),
            CategoryName.of("Shoes"),
            CategoryDescription.of(description),
            parentId != null ? CategoryId.of(parentId) : null,
            true, 5, now, now, "creator", "updater");
    }

    @Test
    @DisplayName("toJpa maps all fields including parentId and description value")
    void toJpa_fullMapping() {
        Category c = category("1", "9", "Footwear");

        CategoryJpaEntity e = mapper.toJpa(c, null);

        assertThat(e.getName()).isEqualTo("Shoes");
        assertThat(e.getDescription()).isEqualTo("Footwear");
        assertThat(e.getParentId()).isEqualTo("9");
        assertThat(e.isActive()).isTrue();
        assertThat(e.getSortOrder()).isEqualTo(5);
        assertThat(e.getCreatedBy()).isEqualTo("creator");
        assertThat(e.getUpdatedBy()).isEqualTo("updater");
        assertThat(e.getCreatedAt()).isEqualTo(Instant.parse("2024-02-02T10:00:00Z"));
    }

    @Test
    @DisplayName("toJpa with null description and null parentId → null columns")
    void toJpa_nullOptionalFields() {
        Category c = category("1", null, null);

        CategoryJpaEntity e = mapper.toJpa(c, null);

        assertThat(e.getDescription()).isNull();
        assertThat(e.getParentId()).isNull();
    }

    @Test
    @DisplayName("toJpa reuses provided entity instance")
    void toJpa_reusesExisting() {
        var existing = new CategoryJpaEntity();
        existing.setId("42");

        CategoryJpaEntity e = mapper.toJpa(category("42", null, "x"), existing);

        assertThat(e).isSameAs(existing);
        assertThat(e.getId()).isEqualTo("42");
    }

    @Test
    @DisplayName("toDomain reconstitutes the aggregate with parentId and description")
    void toDomain_fullMapping() {
        var e = new CategoryJpaEntity();
        e.setId("7");
        e.setName("Boots");
        e.setDescription("Tall boots");
        e.setParentId("3");
        e.setActive(false);
        e.setSortOrder(2);
        Instant now = Instant.parse("2024-03-03T00:00:00Z");
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setCreatedBy("c");
        e.setUpdatedBy("u");

        Category c = mapper.toDomain(e);

        assertThat(c.getId().value()).isEqualTo("7");
        assertThat(c.getName().value()).isEqualTo("Boots");
        assertThat(c.getDescription().value()).isEqualTo("Tall boots");
        assertThat(c.getParentId().value()).isEqualTo("3");
        assertThat(c.isActive()).isFalse();
        assertThat(c.getSortOrder()).isEqualTo(2);
        assertThat(c.getCreatedBy()).isEqualTo("c");
        assertThat(c.getUpdatedBy()).isEqualTo("u");
    }

    @Test
    @DisplayName("toDomain with null parentId → null CategoryId (not wrapped)")
    void toDomain_nullParent() {
        var e = new CategoryJpaEntity();
        e.setId("7");
        e.setName("Root");
        e.setParentId(null);
        e.setActive(true);
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        Category c = mapper.toDomain(e);

        assertThat(c.getParentId()).isNull();
    }

    @Test
    @DisplayName("round-trip toJpa → toDomain preserves identity-bearing fields")
    void roundTrip() {
        Category original = category("1", "9", "Footwear");

        Category back = mapper.toDomain(setId(mapper.toJpa(original, null), "1"));

        assertThat(back.getName().value()).isEqualTo("Shoes");
        assertThat(back.getDescription().value()).isEqualTo("Footwear");
        assertThat(back.getParentId().value()).isEqualTo("9");
        assertThat(back.getSortOrder()).isEqualTo(5);
    }

    private static CategoryJpaEntity setId(CategoryJpaEntity e, String id) {
        e.setId(id);
        return e;
    }
}
