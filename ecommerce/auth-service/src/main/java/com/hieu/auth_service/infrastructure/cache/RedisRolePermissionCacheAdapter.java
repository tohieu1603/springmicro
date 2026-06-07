package com.hieu.auth_service.infrastructure.cache;

import com.hieu.auth_service.application.port.RolePermissionCachePort;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-backed implementation of {@link RolePermissionCachePort}.
 *
 * <p>Storage shape: Redis SET at key {@code role_permissions:{ROLE_NAME}} holding
 * permission names. Using SET (not String) avoids separator-collision bugs and enables
 * O(1) membership checks if callers later need per-permission lookup.
 *
 * <p>TTL = 5 minutes. If an invalidation event is missed (e.g. Kafka consumer offline),
 * the cache self-heals within one TTL window.
 *
 * <p>Warm-up on {@link ApplicationReadyEvent}: loads every role into the cache so the
 * first authenticated request doesn't pay a cold-miss DB query.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRolePermissionCacheAdapter implements RolePermissionCachePort {

    /** Key prefix — shared with downstream services that also hit the same Redis. */
    static final String KEY_PREFIX = "role_permissions:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Set<String> get(String roleName) {
        try {
            Set<String> members = redis.opsForSet().members(KEY_PREFIX + roleName);
            return (members == null || members.isEmpty()) ? null : members;
        } catch (Exception e) {
            log.warn("Redis unavailable for role cache read ({}): {}", roleName, e.getMessage());
            return null;
        }
    }

    @Override
    public void put(String roleName, Set<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            evict(roleName);
            return;
        }
        try {
            String key = KEY_PREFIX + roleName;
            redis.delete(key);
            redis.opsForSet().add(key, permissionNames.toArray(String[]::new));
            redis.expire(key, TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable for role cache write ({}): {}", roleName, e.getMessage());
        }
    }

    @Override
    public void evict(String roleName) {
        try {
            redis.delete(KEY_PREFIX + roleName);
        } catch (Exception e) {
            log.warn("Redis unavailable for role cache evict ({}): {}", roleName, e.getMessage());
        }
    }

    @Override
    public void evictAll() {
        try {
            // H4: Use SCAN instead of KEYS to avoid O(N) blocking on large keyspaces.
            var keys = new java.util.HashSet<String>();
            try (var cursor = redis.getConnectionFactory().getConnection().keyCommands()
                    .scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(KEY_PREFIX + "*").count(100).build())) {
                cursor.forEachRemaining(b -> keys.add(new String(b)));
            }
            if (!keys.isEmpty()) redis.delete(keys);
        } catch (Exception e) {
            log.warn("Redis unavailable for role cache wipe: {}", e.getMessage());
        }
    }

    /**
     * Bulk-loads every role → permissions pair into Redis on app startup.
     * Runs after the context is fully ready so repositories are wired.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            int count = 0;
            for (Role role : roleRepository.findAll()) {
                Set<String> permNames = role.getPermissions().isEmpty()
                        ? Set.of()
                        : permissionRepository.findByIdIn(role.getPermissions()).stream()
                                .map(p -> p.getName().value())
                                .collect(Collectors.toSet());
                put(role.getName().value(), permNames);
                count++;
            }
            log.info("Role-permission cache warm-up: populated {} role(s) into Redis", count);
        } catch (Exception e) {
            log.warn("Role-permission cache warm-up failed: {}", e.getMessage());
        }
    }
}
