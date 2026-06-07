package com.hieu.auth_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseManualIdEntity implements Persistable<String> {

    @Id
    protected String id;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @Transient
    protected boolean isNew = true;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
 
    @PostLoad
    @PostPersist
    protected void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}