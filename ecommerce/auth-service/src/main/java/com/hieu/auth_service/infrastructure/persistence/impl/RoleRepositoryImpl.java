package com.hieu.auth_service.infrastructure.persistence.impl;

import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.PermissionJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RoleJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.PermissionJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.RoleJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.mapper.RoleJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Data JPA adapter for {@link RoleRepository}.
 *
 * <p>Previously a TODO-stub returning input unchanged — now routes through
 * {@link RoleJpaMapper} and correctly resolves permission references from the persistence
 * context so Hibernate updates the {@code role_permissions} join table cleanly.
 */
@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RoleJpaMapper mapper;

    /**
     * Persists or updates a {@link Role}. Resolves referenced permissions to managed
     * entities before saving — otherwise Hibernate would treat them as detached.
     */
    @Override
    @Transactional
    public Role save(Role role) {
        boolean isNew = !jpaRepository.existsById(role.getId().value());

        Set<String> permIds = role.getPermissions().stream()
                .map(PermissionId::value)
                .collect(Collectors.toSet());
        Set<PermissionJpaEntity> permissionEntities = permIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(permissionJpaRepository.findByIdIn(permIds));

        RoleJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(role, permissionEntities, isNew));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(RoleId roleId) {
        return jpaRepository.findByIdWithPermissions(roleId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(RoleName roleName) {
        return jpaRepository.findByNameWithPermissions(roleName.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findByIdIn(Set<RoleId> roleIds) {
        Set<String> ids = roleIds.stream().map(RoleId::value).collect(Collectors.toSet());
        return jpaRepository.findByIdInWithPermissions(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByIdWithPermissions(RoleId roleId) {
        return jpaRepository.findByIdWithPermissions(roleId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByNameWithPermissions(RoleName roleName) {
        return jpaRepository.findByNameWithPermissions(roleName.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(RoleName roleName) {
        return jpaRepository.existsByName(roleName.value());
    }

    @Override
    @Transactional
    public void delete(Role role) {
        if (role.getId() != null) {
            jpaRepository.deleteById(role.getId().value());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return jpaRepository.findAllWithPermissions().stream().map(mapper::toDomain).toList();
    }
}
