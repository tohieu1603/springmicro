package com.hieu.auth_service.domain.models.role;

import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.role.events.PermissionGrantedEvent;
import com.hieu.auth_service.domain.models.role.events.PermissionRevokedEvent;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role aggregate root — a named bundle of permissions.
 *
 * <p>Permission lists are never mutated externally; callers go through {@link #grantPermission}
 * and {@link #revokePermission} so invariants and events stay consistent.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(of = {"id", "name", "description"})
public final class Role extends AggregateRoot {

    @EqualsAndHashCode.Include
    private RoleId id;
    private RoleName name;
    private String description;
    private Set<PermissionId> permissions;
    private Instant createdAt;
    private Instant updatedAt;

    private Role() {
        this.permissions = new HashSet<>();
    }

    public static Role create(RoleName name, String description) {
        Role r = new Role();
        r.id = RoleId.generate();
        r.name = name;
        r.description = description;
        r.createdAt = Instant.now();
        r.updatedAt = r.createdAt;
        return r;
    }

    public static Role reconstitute(RoleId id, RoleName name, String description,
                                    Set<PermissionId> permissions,
                                    Instant createdAt, Instant updatedAt) {
        Role r = new Role();
        r.id = id;
        r.name = name;
        r.description = description;
        r.permissions = new HashSet<>(permissions);
        r.createdAt = createdAt;
        r.updatedAt = updatedAt;
        return r;
    }

    public void updateDescription(String newDescription) {
        if (Objects.equals(description, newDescription)) return;
        this.description = newDescription;
        this.updatedAt = Instant.now();
    }

    public void grantPermission(PermissionId permission) {
        if (permissions.add(permission)) {
            updatedAt = Instant.now();
            registerEvent(new PermissionGrantedEvent(id.value(), permission.value()));
        }
    }

    public void revokePermission(PermissionId permission) {
        if (permissions.remove(permission)) {
            updatedAt = Instant.now();
            registerEvent(new PermissionRevokedEvent(id.value(), permission.value()));
        }
    }

    public boolean hasPermission(PermissionId permission) {
        return permissions.contains(permission);
    }

    public Set<PermissionId> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
}
