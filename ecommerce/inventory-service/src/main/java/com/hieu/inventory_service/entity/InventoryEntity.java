package com.hieu.inventory_service.entity;

import com.hieu.inventory_service.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code inventories} table.
 * All mutation methods enforce invariants; optimistic lock via {@code @Version}.
 */
@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, unique = true, length = 36)
    private String productId;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "min_stock_level", nullable = false)
    @Builder.Default
    private Integer minStockLevel = 10;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        lastUpdated = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    /** Available = total - reserved. */
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean canReserve(int amount) {
        return getAvailableQuantity() >= amount;
    }

    /** Increments reservedQuantity; throws {@link InsufficientStockException} if insufficient. */
    public void reserve(int amount) {
        if (!canReserve(amount)) {
            throw new InsufficientStockException(productId, getAvailableQuantity(), amount);
        }
        reservedQuantity += amount;
    }

    /** Idempotent release — floors at 0. */
    public void releaseReservation(int amount) {
        reservedQuantity = Math.max(0, reservedQuantity - amount);
    }

    /** Decrements both quantity and reservedQuantity atomically. */
    public void confirmReservation(int amount) {
        if (quantity < amount) {
            throw new IllegalStateException(
                String.format("Cannot confirm: quantity=%d < amount=%d", quantity, amount));
        }
        if (reservedQuantity < amount) {
            throw new IllegalStateException(
                String.format("Cannot confirm: reservedQuantity=%d < amount=%d", reservedQuantity, amount));
        }
        quantity -= amount;
        reservedQuantity -= amount;
    }

    public void addStock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        quantity += amount;
    }

    /**
     * Reduces total quantity (e.g. shrinkage, write-off). Caller must ensure
     * {@code quantity - amount >= reservedQuantity} before calling.
     */
    public void subtractStock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (quantity - amount < reservedQuantity) {
            throw new IllegalStateException(
                "Cannot subtract " + amount + " from quantity=" + quantity
                + "; would drop below reservedQuantity=" + reservedQuantity);
        }
        quantity -= amount;
    }

    public boolean isLowStock() {
        return getAvailableQuantity() <= minStockLevel;
    }
}
