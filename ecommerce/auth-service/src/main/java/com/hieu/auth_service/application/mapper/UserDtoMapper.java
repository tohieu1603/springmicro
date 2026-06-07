package com.hieu.auth_service.application.mapper;

import com.hieu.auth_service.application.dto.PermissionDTO;
import com.hieu.auth_service.application.dto.RoleDTO;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct-driven mapper from domain aggregates to read-model DTOs.
 *
 * <p>Generated implementation is wired as a Spring bean
 * ({@code componentModel = "spring"}). Value Object fields are unwrapped via
 * nested {@code source} paths (e.g. {@code user.id.value}); Set projections
 * over collections are produced by {@code @Named} helper methods below.
 */
@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    // ── User → UserDTO (full, with permissions) ────────────────────────────
    @Mapping(target = "id",                    source = "user.id.value")
    @Mapping(target = "username",              source = "user.username.value")
    @Mapping(target = "email",                 source = "user.email.value")
    @Mapping(target = "firstName",             source = "user.personName.firstName")
    @Mapping(target = "lastName",              source = "user.personName.lastName")
    @Mapping(target = "enabled",               source = "user.accountStatus.enabled")
    @Mapping(target = "accountNonExpired",     source = "user.accountStatus.accountNonExpired")
    @Mapping(target = "accountNonLocked",      source = "user.accountStatus.accountNonLocked")
    @Mapping(target = "credentialsNonExpired", source = "user.accountStatus.credentialsNonExpired")
    @Mapping(target = "lastLogin",             source = "user.accountStatus.lastLogin")
    @Mapping(target = "createdAt",             source = "user.createdAt")
    @Mapping(target = "updatedAt",             source = "user.updatedAt")
    @Mapping(target = "roles",                 source = "roles",                 qualifiedByName = "toRoleNames")
    @Mapping(target = "permissions",           source = "effectivePermissions",  qualifiedByName = "toPermissionNames")
    UserDTO toDto(User user, Collection<Role> roles, Collection<Permission> effectivePermissions);

    /** Overload that omits permissions — cheaper projection when caller doesn't need them. */
    default UserDTO toDto(User user, Collection<Role> roles) {
        return toDto(user, roles, List.of());
    }

    // ── Role → RoleDTO ─────────────────────────────────────────────────────
    @Mapping(target = "id",          source = "role.id.value")
    @Mapping(target = "name",        source = "role.name.value")
    @Mapping(target = "description", source = "role.description")
    @Mapping(target = "permissions", source = "grantedPermissions")
    @Mapping(target = "createdAt",   source = "role.createdAt")
    @Mapping(target = "updatedAt",   source = "role.updatedAt")
    RoleDTO toDto(Role role, Set<String> grantedPermissions);

    // ── Permission → PermissionDTO ─────────────────────────────────────────
    @Mapping(target = "id",       source = "id.value")
    @Mapping(target = "name",     source = "name.value")
    @Mapping(target = "resource", source = "name.resource")
    @Mapping(target = "action",   source = "name.action")
    PermissionDTO toDto(Permission permission);

    // ── Helper projections (referenced via qualifiedByName) ────────────────

    @Named("toRoleNames")
    default Set<String> toRoleNames(Collection<Role> roles) {
        if (roles == null || roles.isEmpty()) return Set.of();
        return roles.stream()
                .map(r -> r.getName().value())
                .collect(Collectors.toSet());
    }

    @Named("toPermissionNames")
    default Set<String> toPermissionNames(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) return Set.of();
        return permissions.stream()
                .map(p -> p.getName().value())
                .collect(Collectors.toSet());
    }
}
