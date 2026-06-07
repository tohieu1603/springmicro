package com.hieu.catalog_service.infrastructure.persistence.impl;

import com.hieu.catalog_service.domain.model.category.Category;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.CategoryJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.repositories.CategoryJpaRepository;
import com.hieu.catalog_service.infrastructure.persistence.mapper.CategoryJpaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CategoryRepositoryImpl implements CategoryRepository {

    /** Guard against accidental cycles in legacy data. */
    private static final int MAX_DEPTH = 10;

    private final CategoryJpaRepository jpa;
    private final CategoryJpaMapper mapper;

    @Override
    public Category save(Category category) {
        CategoryJpaEntity existing = category.getId() != null
            ? jpa.findById(category.getId().value()).orElse(null)
            : null;
        CategoryJpaEntity saved = jpa.save(mapper.toJpa(category, existing));
        if (category.getId() == null) category.assignId(CategoryId.of(saved.getId()));
        return category;
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(CategoryName name) {
        return jpa.existsByNameIgnoreCase(name.value());
    }

    @Override
    public boolean existsById(CategoryId id) {
        return id.value() != null && jpa.existsById(id.value());
    }

    @Override
    public List<Category> findAllActive() {
        return jpa.findAllActive().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Category> findByParent(CategoryId parentId) {
        List<CategoryJpaEntity> rows = parentId == null
            ? jpa.findByParentIdIsNullOrderBySortOrderAscIdAsc()
            : jpa.findByParentIdOrderBySortOrderAscIdAsc(parentId.value());
        return rows.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Category> findAncestors(CategoryId id) {
        // Walk parent chain in-process — adjacency model, tree depth is small.
        // Guard against accidental cycles in legacy data.
        List<Category> chain = new ArrayList<>();
        Optional<CategoryJpaEntity> cursor = jpa.findById(id.value());
        int depth = 0;
        while (cursor.isPresent() && depth < MAX_DEPTH) {
            CategoryJpaEntity e = cursor.get();
            chain.add(mapper.toDomain(e));
            cursor = e.getParentId() != null ? jpa.findById(e.getParentId()) : Optional.empty();
            depth++;
        }
        if (depth >= MAX_DEPTH) {
            log.warn("Category ancestor chain hit MAX_DEPTH={} starting from id={}", MAX_DEPTH, id);
        }
        return chain;
    }

    @Override
    public void delete(Category category) {
        if (category.getId() != null) jpa.deleteById(category.getId().value());
    }
}
