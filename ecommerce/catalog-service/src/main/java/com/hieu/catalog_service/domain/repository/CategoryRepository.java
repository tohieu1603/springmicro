package com.hieu.catalog_service.domain.repository;

import com.hieu.catalog_service.domain.model.category.Category;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(CategoryId id);

    boolean existsByName(CategoryName name);

    boolean existsById(CategoryId id);

    /** Full tree — ordered by parent/sort — for the {@code GET /categories} endpoint. */
    List<Category> findAllActive();

    /** Children of a given parent (or root-level if {@code parentId} is {@code null}). */
    List<Category> findByParent(CategoryId parentId);

    /** Ancestor chain (including self) — used to detect cycles before re-parenting. */
    List<Category> findAncestors(CategoryId id);

    void delete(Category category);
}
