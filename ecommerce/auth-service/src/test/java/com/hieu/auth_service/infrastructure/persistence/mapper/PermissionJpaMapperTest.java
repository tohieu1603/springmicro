package com.hieu.auth_service.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.PermissionJpaEntity;

/**
 * Pure unit tests for {@link PermissionJpaMapper}: PermissionName value-object unpacking into the
 * flat {@code name/resource/action} entity columns, reconstruction back into the value object,
 * in-place update, and null guards.
 */
class PermissionJpaMapperTest {

    private final PermissionJpaMapper mapper = new PermissionJpaMapper();

    private static final String PERM_ID = "99999999-9999-9999-9999-999999999999";

    private Permission domainPermission() {
        return Permission.reconstitute(
                PermissionId.of(PERM_ID),
                "product",          // lower-case input → uppercased by PermissionName
                "update",
                "Update products",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-02-02T00:00:00Z"));
    }

    @Test
    void toJpaEntity_unpacksPermissionNameIntoColumns() {
        PermissionJpaEntity entity = mapper.toJpaEntity(domainPermission(), true);

        assertThat(entity.getId()).isEqualTo(PERM_ID);
        assertThat(entity.getName()).isEqualTo("PRODUCT_UPDATE");
        assertThat(entity.getResource()).isEqualTo("PRODUCT");
        assertThat(entity.getAction()).isEqualTo("UPDATE");
        assertThat(entity.getDescription()).isEqualTo("Update products");
        assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2024-02-02T00:00:00Z"));
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void toDomain_rebuildsPermissionNameFromResourceAndAction() {
        PermissionJpaEntity entity = mapper.toJpaEntity(domainPermission(), false);

        Permission domain = mapper.toDomain(entity);

        assertThat(domain.getId().value()).isEqualTo(PERM_ID);
        assertThat(domain.getName().value()).isEqualTo("PRODUCT_UPDATE");
        assertThat(domain.getName().resource()).isEqualTo("PRODUCT");
        assertThat(domain.getName().action()).isEqualTo("UPDATE");
        assertThat(domain.getDescription()).isEqualTo("Update products");
    }

    @Test
    void roundTrip_preservesState() {
        Permission original = domainPermission();

        Permission rebuilt = mapper.toDomain(mapper.toJpaEntity(original, true));

        assertThat(rebuilt.getId()).isEqualTo(original.getId());
        assertThat(rebuilt.getName()).isEqualTo(original.getName());
        assertThat(rebuilt.getDescription()).isEqualTo(original.getDescription());
    }

    @Test
    void updateJpaEntity_appliesMutationsInPlace() {
        PermissionJpaEntity managed = mapper.toJpaEntity(domainPermission(), false);
        Permission changed = Permission.reconstitute(
                PermissionId.of(PERM_ID),
                "order",
                "delete",
                "Delete orders",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-09-09T00:00:00Z"));

        mapper.updateJpaEntity(changed, managed);

        assertThat(managed.getName()).isEqualTo("ORDER_DELETE");
        assertThat(managed.getResource()).isEqualTo("ORDER");
        assertThat(managed.getAction()).isEqualTo("DELETE");
        assertThat(managed.getDescription()).isEqualTo("Delete orders");
        assertThat(managed.getUpdatedAt()).isEqualTo(Instant.parse("2024-09-09T00:00:00Z"));
    }

    @Test
    void nullInputs_returnNull() {
        assertThat(mapper.toJpaEntity(null, true)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
