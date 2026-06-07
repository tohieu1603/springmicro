package com.hieu.flash_sale_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** JPA entity mapped to the {@code flash_sales} table. */
@Entity
@Table(
    name = "flash_sales",
    indexes = {
        @Index(name = "ix_flashsale_status",  columnList = "status"),
        @Index(name = "ix_flashsale_start",   columnList = "start_time"),
        @Index(name = "ix_flashsale_product", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FlashSaleJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "original_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "total_slots", nullable = false)
    private int totalSlots;

    @Column(name = "reserved_slots", nullable = false)
    private int reservedSlots = 0;

    @Column(name = "max_per_user", nullable = false)
    private int maxPerUser = 1;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private FlashSaleStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = java.util.UUID.randomUUID().toString();
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
