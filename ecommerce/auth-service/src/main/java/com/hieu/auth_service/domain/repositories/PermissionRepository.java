package com.hieu.auth_service.domain.repositories;


import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.permission.vo.PermissionName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Domain Repository Interface for Permission Entity
 */
public interface PermissionRepository {

    /**
     * Save a permission (insert or update)
     */
    Permission save(Permission permission);

    /**
     * Find permission by ID
     */
    Optional<Permission> findById(PermissionId permissionId);

    /**
     * Find permission by name
     */
    Optional<Permission> findByName(PermissionName permissionName);

    /**
     * Find multiple permissions by IDs
     */
    List<Permission> findByIdIn(Set<PermissionId> permissionIds);

    /**
     * Find permissions by resource
     */
    List<Permission> findByResource(String resource);

    /**
     * Find permissions by action
     */
    List<Permission> findByAction(String action);

    /**
     * Find permission by resource and action
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /**
     * Check if permission name exists
     */
    boolean existsByName(PermissionName permissionName);

    /**
     * Delete permission
     */
    void delete(Permission permission);

    /**
     * Find all permissions
     */
    List<Permission> findAll();
}
