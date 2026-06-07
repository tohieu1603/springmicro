package com.hieu.auth_service.domain.models.permission;

import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.permission.vo.PermissionName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;


/**
 * Permission Entity (part of Role Aggregate)
 * Represents a specific permission that can be granted to roles
 */

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(of = {"id", "name", "description"})
public class Permission {

    @EqualsAndHashCode.Include
    private PermissionId id;
    private PermissionName name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;


    /**
     * Factory method new permission
     * @param resource
     * @param action
     * @param description
     * @return
     */
    public static Permission create(
            String resource,
            String action,
            String description
    ) {
        Permission permission = new Permission();
        permission.id = PermissionId.generate();
        permission.name = PermissionName.of(resource, action);
        permission.description = description;
        permission.createdAt = Instant.now();
        permission.updatedAt = Instant.now();

        return permission;
    }

    /**
     * Factory method: Reconstitute from persistence
     * @param id
     * @param resource
     * @param action
     * @param description
     * @param createdAt
     * @param updatedAt
     * @return
     */
    public static Permission reconstitute(
            PermissionId id,
            String resource,
            String action,
            String description,
            Instant createdAt,
            Instant updatedAt
    ) {
        Permission permission = new Permission();
        permission.id = id;
        permission.name = PermissionName.of(resource, action);
        permission.description = description;
        permission.createdAt = createdAt;
        permission.updatedAt = updatedAt;

        return permission;
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if this permission grants access to a specific resource and action
     * @param resource
     * @param action
     * @return
     */
    public boolean grants(String resource, String action) {
        return name.isForResource(resource) && name.allowsAction(action);
    }
}
