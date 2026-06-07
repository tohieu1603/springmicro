package com.hieu.catalog_service.domain.model.category;

import com.hieu.catalog_service.domain.events.category.CategoryDeletedEvent;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryDescription;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Category aggregate (unit)")
class CategoryTest {

    private static Category persisted(String id) {
        return Category.reconstitute(
                CategoryId.of(id), CategoryName.of("Electronics"), CategoryDescription.of("desc"),
                null, true, 0, Instant.now(), Instant.now(), "creator", "creator");
    }

    @Test
    @DisplayName("create() yields an active category")
    void create() {
        Category c = Category.create(CategoryName.of("Books"), CategoryDescription.of(null), null, 1, "admin");
        assertThat(c.isActive()).isTrue();
        assertThat(c.getName().value()).isEqualTo("Books");
    }

    @Test
    @DisplayName("a category cannot be its own parent")
    void selfParentRejected() {
        Category c = persisted("1");
        assertThatThrownBy(() -> c.update(
                CategoryName.of("Electronics"), CategoryDescription.of("d"), CategoryId.of("1"), 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update() applies a new name and a different parent")
    void update() {
        Category c = persisted("1");
        c.update(CategoryName.of("Phones"), CategoryDescription.of("d"), CategoryId.of("2"), 5, "admin");
        assertThat(c.getName().value()).isEqualTo("Phones");
        assertThat(c.getParentId()).isEqualTo(CategoryId.of("2"));
        assertThat(c.getSortOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("deactivate()/activate() toggle the active flag")
    void toggleActive() {
        Category c = persisted("1");
        c.deactivate("admin");
        assertThat(c.isActive()).isFalse();
        c.activate("admin");
        assertThat(c.isActive()).isTrue();
    }

    @Test
    @DisplayName("softDelete() deactivates and raises a deleted event")
    void softDelete() {
        Category c = persisted("1");
        c.softDelete("admin");
        assertThat(c.isActive()).isFalse();
        assertThat(c.peekDomainEvents()).anyMatch(e -> e instanceof CategoryDeletedEvent);
    }
}
