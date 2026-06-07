package com.hieu.auth_service.infrastructure.persistence.mapper;

import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.PermissionJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer between the {@code Permission} aggregate and {@link PermissionJpaEntity}.
 */
@Component
public class PermissionJpaMapper {

    /** Projects a domain {@link Permission} onto a persistable JPA entity. */
    public PermissionJpaEntity toJpaEntity(Permission permission, boolean isNew) {
        if (permission == null) return null;
        PermissionJpaEntity entity = PermissionJpaEntity.builder()
                .id(permission.getId().value())
                .name(permission.getName().value())
                .resource(permission.getName().resource())
                .action(permission.getName().action())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
        entity.setNew(isNew);
        return entity;
    }

    /** Rebuilds a domain {@link Permission} from its JPA representation. */
    public Permission toDomain(PermissionJpaEntity entity) {
        if (entity == null) return null;
        return Permission.reconstitute(
                PermissionId.of(entity.getId()),
                entity.getResource(),
                entity.getAction(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /** Applies domain mutations onto a managed entity (UPDATE path). */
    public void updateJpaEntity(Permission permission, PermissionJpaEntity entity) {
        entity.setName(permission.getName().value());
        entity.setResource(permission.getName().resource());
        entity.setAction(permission.getName().action());
        entity.setDescription(permission.getDescription());
        entity.setUpdatedAt(permission.getUpdatedAt());
    }
}
