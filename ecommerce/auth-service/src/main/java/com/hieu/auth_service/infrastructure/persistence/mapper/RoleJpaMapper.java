package com.hieu.auth_service.infrastructure.persistence.mapper;

import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.PermissionJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RoleJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Anti-Corruption Layer between the {@code Role} aggregate and {@link RoleJpaEntity}.
 *
 * <p>Permission associations are represented as a {@code Set<PermissionId>} in the domain
 * but as a {@code Set<PermissionJpaEntity>} in JPA — the mapper bridges the two.
 */
@Component
public class RoleJpaMapper {

    /**
     * Projects a domain {@link Role} onto a persistable {@link RoleJpaEntity}.
     *
     * @param role              source domain aggregate
     * @param permissionEntities JPA permission entities already resolved by the caller
     * @return populated JPA entity
     */
    public RoleJpaEntity toJpaEntity(Role role, Set<PermissionJpaEntity> permissionEntities, boolean isNew) {
        if (role == null) return null;
        RoleJpaEntity entity = RoleJpaEntity.builder()
                .id(role.getId().value())
                .name(role.getName().value())
                .description(role.getDescription())
                .permissions(permissionEntities)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
        entity.setNew(isNew);
        return entity;
    }

    /** Rebuilds a domain {@link Role} from its JPA representation. */
    public Role toDomain(RoleJpaEntity entity) {
        if (entity == null) return null;
        Set<PermissionId> permissionIds = entity.getPermissions().stream()
                .map(p -> PermissionId.of(p.getId()))
                .collect(Collectors.toSet());

        return Role.reconstitute(
                RoleId.of(entity.getId()),
                RoleName.of(entity.getName()),
                entity.getDescription(),
                permissionIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /**
     * Applies domain mutations onto a managed entity (UPDATE path, dirty-checking).
     *
     * @param role                source domain aggregate
     * @param entity              managed JPA entity
     * @param permissionEntities  resolved permission entities
     */
    public void updateJpaEntity(Role role, RoleJpaEntity entity, Set<PermissionJpaEntity> permissionEntities) {
        entity.setName(role.getName().value());
        entity.setDescription(role.getDescription());
        entity.setPermissions(permissionEntities);
        entity.setUpdatedAt(role.getUpdatedAt());
    }
}
