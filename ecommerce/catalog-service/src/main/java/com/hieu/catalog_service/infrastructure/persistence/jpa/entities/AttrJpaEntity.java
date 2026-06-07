package com.hieu.catalog_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "attrs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttrJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * Child values — orphan-removal so {@code removeValue(String)} translates to a DELETE
     * without an explicit call on the child repository.
     */
    @OneToMany(mappedBy = "attr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<AttrValJpaEntity> values = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
