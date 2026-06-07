package com.hieu.catalog_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "variants")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VariantJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @Column(nullable = false, length = 64, unique = true)
    private String sku;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(precision = 19, scale = 2)
    private BigDecimal cost;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(length = 1024)
    private String image;

    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 16)
    private String status;

    @Version
    private Long version;

    /**
     * {@code @BatchSize} side-steps Hibernate's "multiple bags" error: we can only
     * {@code JOIN FETCH} one collection (variants) per query, and attrs are batched via
     * a follow-up {@code IN (?,?,?)} that keeps the total queries at O(1) instead of N+1.
     */
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<VariantAttrJpaEntity> attrs = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
