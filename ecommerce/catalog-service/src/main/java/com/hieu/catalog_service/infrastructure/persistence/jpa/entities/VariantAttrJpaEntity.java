package com.hieu.catalog_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "variant_attrs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VariantAttrJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private VariantJpaEntity variant;

    @Column(name = "attr_id", nullable = false, length = 36)
    private String attrId;

    @Column(name = "attr_code", nullable = false, length = 64)
    private String attrCode;

    @Column(name = "attr_name", nullable = false, length = 100)
    private String attrName;

    @Column(name = "val_id", length = 36)
    private String valId;

    @Column(name = "val_text", length = 255)
    private String valText;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
