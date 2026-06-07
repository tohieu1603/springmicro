package com.hieu.auth_service.infrastructure.messaging;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hieu.auth_service.application.port.RolePermissionCachePort;
import com.hieu.auth_service.domain.events.DomainEvent;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.events.PermissionGrantedEvent;
import com.hieu.auth_service.domain.models.role.events.PermissionRevokedEvent;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Keeps the role-permission Redis cache in sync with role mutations.
 *
 * <p>Listens to in-process {@link PermissionGrantedEvent} / {@link PermissionRevokedEvent}
 * (same event bus that feeds {@link KafkaIntegrationEventPublisher}). On
 * {@link TransactionPhase#AFTER_COMMIT} the affected role is re-fetched and the cache
 * entry is rebuilt — a single atomic replace is cheaper + safer than partial updates.
 *
 * <p>Only in-process invalidation is handled here. A sibling auth-service instance on
 * another JVM consumes the Kafka integration event separately (pending outbox pattern);
 * their copies of Redis self-heal within the 5-minute TTL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RolePermissionCacheInvalidator {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionCachePort cache;

    /** Rebuild cache entry after a permission is granted to a role. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPermissionGranted(PermissionGrantedEvent event) {
        refresh(event);
    }

    /** Rebuild cache entry after a permission is revoked from a role. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPermissionRevoked(PermissionRevokedEvent event) {
        refresh(event);
    }

    /**
     * Loads the role + its current permissions and overwrites the cached entry.
     * Safe to call on missing roles — will just evict in that case.
     */
    private void refresh(DomainEvent event) {
        String roleIdRaw = event.aggregateId();
        try {
            Role role = roleRepository.findById(RoleId.of(roleIdRaw)).orElse(null);
            if (role == null) {
                log.debug("Role {} no longer exists; leaving cache as-is", roleIdRaw);
                return;
            }
            Set<String> permissionNames = role.getPermissions().isEmpty()
                    ? Set.of()
                    : permissionRepository.findByIdIn(role.getPermissions()).stream()
                            .map(p -> p.getName().value())
                            .collect(Collectors.toSet());
            cache.put(role.getName().value(), permissionNames);
            log.info("Role-permission cache refreshed for role={} ({} perms)",
                    role.getName().value(), permissionNames.size());
        } catch (Exception e) {
            log.warn("Failed to refresh role-permission cache for {}; evicting instead. Error: {}",
                    roleIdRaw, e.getMessage());
            // Best-effort evict — next read will reload via DB and repopulate.
            cache.evictAll();
        }
    }
}
