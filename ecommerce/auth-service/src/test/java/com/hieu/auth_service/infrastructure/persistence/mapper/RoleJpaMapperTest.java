package com.hieu.auth_service.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.PermissionJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RoleJpaEntity;

/**
 * Pure unit tests for {@link RoleJpaMapper}: Role aggregate <-> entity mapping, the
 * Set&lt;PermissionId&gt; <-> Set&lt;PermissionJpaEntity&gt; bridge, RoleName prefix normalisation,
 * in-place update, and null guards.
 */
class RoleJpaMapperTest {

    private final RoleJpaMapper mapper = new RoleJpaMapper();

    private static final String ROLE_ID = "77777777-7777-7777-7777-777777777777";
    private static final String PERM_ID = "88888888-8888-8888-8888-888888888888";

    private Role domainRole() {
        return Role.reconstitute(
                RoleId.of(ROLE_ID),
                RoleName.of("ADMIN"),                 // normalised to ROLE_ADMIN
                "Administrators",
                Set.of(PermissionId.of(PERM_ID)),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-02-02T00:00:00Z"));
    }

    private PermissionJpaEntity permEntity() {
        return PermissionJpaEntity.builder().id(PERM_ID).name("USER_READ").resource("USER").action("READ").build();
    }

    @Test
    void toJpaEntity_mapsAllFields() {
        RoleJpaEntity entity = mapper.toJpaEntity(domainRole(), Set.of(permEntity()), true);

        assertThat(entity.getId()).isEqualTo(ROLE_ID);
        assertThat(entity.getName()).isEqualTo("ROLE_ADMIN");
        assertThat(entity.getDescription()).isEqualTo("Administrators");
        assertThat(entity.getPermissions()).hasSize(1);
        assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2024-02-02T00:00:00Z"));
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void toJpaEntity_isNewFalseSetsPersistableFlag() {
        RoleJpaEntity entity = mapper.toJpaEntity(domainRole(), Set.of(permEntity()), false);
        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void toDomain_reconstitutesAggregate() {
        RoleJpaEntity entity = mapper.toJpaEntity(domainRole(), Set.of(permEntity()), true);

        Role domain = mapper.toDomain(entity);

        assertThat(domain.getId().value()).isEqualTo(ROLE_ID);
        assertThat(domain.getName().value()).isEqualTo("ROLE_ADMIN");
        assertThat(domain.getDescription()).isEqualTo("Administrators");
        assertThat(domain.getPermissions()).extracting(PermissionId::value).containsExactly(PERM_ID);
    }

    @Test
    void roundTrip_preservesState() {
        Role original = domainRole();

        Role rebuilt = mapper.toDomain(mapper.toJpaEntity(original, Set.of(permEntity()), true));

        assertThat(rebuilt.getId()).isEqualTo(original.getId());
        assertThat(rebuilt.getName()).isEqualTo(original.getName());
        assertThat(rebuilt.getPermissions()).isEqualTo(original.getPermissions());
    }

    @Test
    void updateJpaEntity_appliesMutationsInPlace() {
        RoleJpaEntity managed = mapper.toJpaEntity(domainRole(), Set.of(permEntity()), false);
        Role changed = Role.reconstitute(
                RoleId.of(ROLE_ID),
                RoleName.of("ROLE_MANAGER"),
                "Managers",
                Set.of(),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-09-09T00:00:00Z"));

        mapper.updateJpaEntity(changed, managed, Set.of());

        assertThat(managed.getName()).isEqualTo("ROLE_MANAGER");
        assertThat(managed.getDescription()).isEqualTo("Managers");
        assertThat(managed.getPermissions()).isEmpty();
        assertThat(managed.getUpdatedAt()).isEqualTo(Instant.parse("2024-09-09T00:00:00Z"));
    }

    @Test
    void nullInputs_returnNull() {
        assertThat(mapper.toJpaEntity(null, Set.of(), true)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
