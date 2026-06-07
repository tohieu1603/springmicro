package com.hieu.auth_service.infrastructure.persistence.impl;

import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.permission.vo.PermissionName;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.PermissionJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.PermissionJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.mapper.PermissionJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Data JPA adapter for {@link PermissionRepository}.
 *
 * <p>Previous version had a {@code save()} that returned its input unchanged — a silent
 * data-loss bug. This implementation routes through {@link PermissionJpaMapper} for every
 * direction (domain↔entity) and correctly persists on INSERT/UPDATE.
 */
@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final PermissionJpaMapper mapper;

    /**
     * Persists a {@link Permission}. Relies on Spring Data's merge/persist heuristic
     * (generated UUID id means new entities are simply persisted on first save).
     */
    @Override
    @Transactional
    public Permission save(Permission permission) {
        boolean isNew = !jpaRepository.existsById(permission.getId().value());
        PermissionJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(permission, isNew));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findById(PermissionId permissionId) {
        return jpaRepository.findById(permissionId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findByName(PermissionName permissionName) {
        return jpaRepository.findByName(permissionName.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByIdIn(Set<PermissionId> permissionIds) {
        Set<String> ids = permissionIds.stream().map(PermissionId::value).collect(Collectors.toSet());
        return jpaRepository.findByIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByResource(String resource) {
        return jpaRepository.findByResource(resource).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByAction(String action) {
        return jpaRepository.findByAction(action).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findByResourceAndAction(String resource, String action) {
        return jpaRepository.findByResourceAndAction(resource, action).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(PermissionName permissionName) {
        return jpaRepository.existsByName(permissionName.value());
    }

    @Override
    @Transactional
    public void delete(Permission permission) {
        if (permission.getId() != null) {
            jpaRepository.deleteById(permission.getId().value());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
