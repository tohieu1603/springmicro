package com.hieu.auth_service.domain.repositories;

import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.role.vo.RoleName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Domain Repository Interface for Role Aggregate
 */
public interface RoleRepository {

    /**
     * Save a role (insert or update)
     */
    Role save(Role role);

    /**
     * Find role by ID
     */
    Optional<Role> findById(RoleId roleId);

    /**
     * Find role by name
     */
    Optional<Role> findByName(RoleName roleName);

    /**
     * Find multiple roles by IDs
     */
    List<Role> findByIdIn(Set<RoleId> roleIds);

    /**
     * Find role with permissions loaded
     */
    Optional<Role> findByIdWithPermissions(RoleId roleId);

    /**
     * Find role by name with permissions loaded
     */
    Optional<Role> findByNameWithPermissions(RoleName roleName);

    /**
     * Check if role name exists
     */
    boolean existsByName(RoleName roleName);

    /**
     * Delete role
     */
    void delete(Role role);

    /**
     * Find all roles
     */
    List<Role> findAll();
}
