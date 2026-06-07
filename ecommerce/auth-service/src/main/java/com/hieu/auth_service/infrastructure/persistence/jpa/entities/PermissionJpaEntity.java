package com.hieu.auth_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Permission row.
 *
 * <p>Extends {@link BaseManualIdEntity} so the id is owned by the domain (UUID
 * assigned in {@code Permission.create()}). Mixing {@code @GeneratedValue} with a
 * manually-set id leads to {@code StaleObjectStateException} on save — previous bug.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionJpaEntity extends BaseManualIdEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String resource;

    @Column(length = 50)
    private String action;

    @Column(length = 255)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public PermissionJpaEntity(String id, String name, String resource, String action,
                               String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.isNew = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
