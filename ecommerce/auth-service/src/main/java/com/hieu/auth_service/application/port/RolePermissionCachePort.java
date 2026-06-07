package com.hieu.auth_service.application.port;

import java.util.Set;

public interface RolePermissionCachePort {

    /**
     * Returns the cached permission names for a role, or {@code null} on cache miss.
     *
     * @param roleName canonical role name (e.g. {@code ROLE_USER})
     * @return set of permission names, or {@code null} when the cache has no entry
     */
    Set<String> get(String roleName);

    /**
     * Stores (or overwrites) the cache entry for a role.
     *
     * @param roleName         canonical role name
     * @param permissionNames  permissions granted through this role (defensive copy stored)
     */
    void put(String roleName, Set<String> permissionNames);

    /** Removes a single role from the cache. */
    void evict(String roleName);

    /** Wipes the entire cache — used on bulk permission changes. */
    void evictAll();
}
