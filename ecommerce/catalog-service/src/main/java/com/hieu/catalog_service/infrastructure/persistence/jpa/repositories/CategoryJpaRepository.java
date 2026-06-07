package com.hieu.catalog_service.infrastructure.persistence.jpa.repositories;

import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, String> {

    @Query("SELECT COUNT(c) > 0 FROM CategoryJpaEntity c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.active = true ORDER BY c.parentId NULLS FIRST, c.sortOrder, c.id")
    List<CategoryJpaEntity> findAllActive();

    List<CategoryJpaEntity> findByParentIdOrderBySortOrderAscIdAsc(String parentId);

    List<CategoryJpaEntity> findByParentIdIsNullOrderBySortOrderAscIdAsc();
}
