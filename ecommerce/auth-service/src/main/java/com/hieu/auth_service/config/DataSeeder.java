package com.hieu.auth_service.config;

import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionName;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/**
 * Bootstrap data — default permissions and roles required for the service to function.
 *
 * <p>Idempotent: looks up each item by name and inserts only what's missing. Safe to
 * run on every startup, including production. The {@code @Profile} guard keeps this
 * seed active everywhere except {@code test} (tests supply their own fixtures).
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    /** Canonical resource + action combinations defining the base permission set. */
    private static final List<PermissionSeed> DEFAULT_PERMISSIONS = List.of(
            new PermissionSeed("USER",       "READ",   "Read user data"),
            new PermissionSeed("USER",       "WRITE",  "Create / update users"),
            new PermissionSeed("USER",       "DELETE", "Delete users"),
            new PermissionSeed("ROLE",       "MANAGE", "Create / update / delete roles"),
            new PermissionSeed("PERMISSION", "MANAGE", "Create / update / delete permissions"));

    /**
     * Boot-time seeder. Runs after Spring Data is fully initialised so repositories are ready.
     *
     * <p>The whole seed runs inside an explicit {@link TransactionTemplate} rather than a
     * {@code @Transactional} method: the runner used to self-invoke {@code seed(...)} on the same
     * bean, which bypasses the Spring AOP proxy and silently dropped the transaction boundary —
     * leaving half-baked role/permission graphs on a startup crash.
     *
     * @param permissionRepository permission aggregate repository
     * @param roleRepository       role aggregate repository
     * @param transactionManager   platform transaction manager backing the seed transaction
     * @return {@link ApplicationRunner} executed once per JVM start
     */
    @Bean
    public ApplicationRunner seedDefaultRolesAndPermissions(PermissionRepository permissionRepository,
                                                            RoleRepository roleRepository,
                                                            PlatformTransactionManager transactionManager) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return args -> tx.executeWithoutResult(status -> seed(permissionRepository, roleRepository));
    }

    /**
     * Seeds default permissions/roles. Always invoked within the {@link TransactionTemplate}
     * created by {@link #seedDefaultRolesAndPermissions} so it commits atomically.
     */
    protected void seed(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        // 1. Permissions — chỉ INSERT khi chưa tồn tại; return value của orElseGet
        // không dùng đến nhưng vẫn cần `ifPresentOrElse` hoặc tương đương để Sonar
        // (rule S2201) không phàn nàn. Đổi sang ifPresentOrElse cho rõ ý.
        for (PermissionSeed seed : DEFAULT_PERMISSIONS) {
            PermissionName name = PermissionName.of(seed.resource(), seed.action());
            permissionRepository.findByName(name).ifPresentOrElse(
                existing -> { /* đã có — không làm gì */ },
                () -> {
                    Permission created = permissionRepository.save(
                            Permission.create(seed.resource(), seed.action(), seed.description()));
                    log.info("[seed] Inserted permission {}", created.getName().value());
                }
            );
        }

        // 2. ROLE_USER — baseline read-only authority for every registered user
        ensureRole(roleRepository, permissionRepository, "ROLE_USER",
                "Default role for authenticated end users",
                List.of("USER_READ"));

        // 3. ROLE_ADMIN — full control, used for operational dashboards
        ensureRole(roleRepository, permissionRepository, "ROLE_ADMIN",
                "Administrator with full system access",
                DEFAULT_PERMISSIONS.stream()
                        .map(s -> PermissionName.of(s.resource(), s.action()).value())
                        .toList());
    }

    /**
     * Ensures a role with the supplied name exists and carries the specified permissions.
     * If the role already exists, missing permissions are granted incrementally.
     */
    private void ensureRole(RoleRepository roleRepository,
                             PermissionRepository permissionRepository,
                             String roleNameRaw,
                             String description,
                             List<String> permissionNames) {
        RoleName name = RoleName.of(roleNameRaw);
        Role role = roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role r = roleRepository.save(Role.create(name, description));
                    log.info("[seed] Inserted role {}", r.getName().value());
                    return r;
                });

        permissionNames.stream()
                .map(PermissionName::fromString)
                .map(permissionRepository::findByName)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(p -> !role.hasPermission(p.getId()))
                .forEach(p -> role.grantPermission(p.getId()));

        if (role.peekDomainEvents().stream()
                .anyMatch(Objects::nonNull)) {
            roleRepository.save(role);
        }
    }

    /** Seed record — captures the canonical shape of a permission on startup. */
    private record PermissionSeed(String resource, String action, String description) {}
}
