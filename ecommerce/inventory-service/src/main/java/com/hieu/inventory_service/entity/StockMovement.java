package com.hieu.inventory_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit row for stock-changing ops.
 *
 * <p>Kept separate from {@link InventoryEntity} so deleting an inventory row
 * doesn't drop the historical trail (useful for legal / accounting audits).
 * Indexed by (product_id, created_at DESC) so admin "show recent movements"
 * queries hit the index without sorting in memory.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    public enum Reason { ADJUST, RESERVE, CONFIRM, RELEASE, CREATE }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer delta;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "reserved_after", nullable = false)
    private Integer reservedAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Reason reason;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "actor")
    private String actor;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
