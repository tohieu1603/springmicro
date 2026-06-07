package com.hieu.catalog_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "attr_vals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttrValJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_id", nullable = false)
    private AttrJpaEntity attr;

    @Column(nullable = false, length = 100)
    private String val;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
