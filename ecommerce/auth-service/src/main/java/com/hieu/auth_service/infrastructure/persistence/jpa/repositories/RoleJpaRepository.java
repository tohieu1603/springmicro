package com.hieu.auth_service.infrastructure.persistence.jpa.repositories;

import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    Optional<RoleJpaEntity> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM RoleJpaEntity r LEFT JOIN FETCH r.permissions WHERE r.id IN :ids")
    Set<RoleJpaEntity> findByIdIn(Set<String> ids);

    @Query("SELECT r FROM RoleJpaEntity r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<RoleJpaEntity> findByIdWithPermissions(@Param("id") String id);

    @Query("SELECT r FROM RoleJpaEntity r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<RoleJpaEntity> findByNameWithPermissions(@Param("name") String name);

    @Query("SELECT DISTINCT r FROM RoleJpaEntity r LEFT JOIN FETCH r.permissions")
    List<RoleJpaEntity> findAllWithPermissions();

    @Query("SELECT DISTINCT r FROM RoleJpaEntity r LEFT JOIN FETCH r.permissions WHERE r.id IN :ids")
    List<RoleJpaEntity> findByIdInWithPermissions(@Param("ids") Set<String> ids);
}