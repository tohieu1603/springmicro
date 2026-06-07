package com.hieu.catalog_service.infrastructure.persistence.mapper;

import com.hieu.catalog_service.domain.model.category.Category;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryDescription;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.CategoryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryJpaMapper {

    public CategoryJpaEntity toJpa(Category c, CategoryJpaEntity existing) {
        CategoryJpaEntity e = existing != null ? existing : new CategoryJpaEntity();
        e.setName(c.getName().value());
        e.setDescription(c.getDescription() != null ? c.getDescription().value() : null);
        e.setParentId(c.getParentId() != null ? c.getParentId().value() : null);
        e.setActive(c.isActive());
        e.setSortOrder(c.getSortOrder());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        e.setCreatedBy(c.getCreatedBy());
        e.setUpdatedBy(c.getUpdatedBy());
        return e;
    }

    public Category toDomain(CategoryJpaEntity e) {
        return Category.reconstitute(
            CategoryId.of(e.getId()),
            CategoryName.of(e.getName()),
            CategoryDescription.of(e.getDescription()),
            e.getParentId() != null ? CategoryId.of(e.getParentId()) : null,
            e.isActive(),
            e.getSortOrder(),
            e.getCreatedAt(),
            e.getUpdatedAt(),
            e.getCreatedBy(),
            e.getUpdatedBy()
        );
    }
}
