package com.hieu.catalog_service.infrastructure.persistence.jpa.repositories;

import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.AttrJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttrJpaRepository extends JpaRepository<AttrJpaEntity, String> {

    @Query("SELECT a FROM AttrJpaEntity a LEFT JOIN FETCH a.values WHERE a.id = :id")
    Optional<AttrJpaEntity> findByIdWithValues(@Param("id") String id);

    @Query("SELECT a FROM AttrJpaEntity a LEFT JOIN FETCH a.values WHERE a.code = :code")
    Optional<AttrJpaEntity> findByCodeWithValues(@Param("code") String code);

    boolean existsByCode(String code);

    @Query("SELECT DISTINCT a FROM AttrJpaEntity a LEFT JOIN FETCH a.values ORDER BY a.sortOrder, a.id")
    List<AttrJpaEntity> findAllWithValues();

    @Query("SELECT DISTINCT a FROM AttrJpaEntity a LEFT JOIN FETCH a.values WHERE a.id IN :ids")
    List<AttrJpaEntity> findAllByIdsWithValues(@Param("ids") List<String> ids);
}
