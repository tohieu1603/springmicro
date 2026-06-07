package com.hieu.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists reservation state per order for idempotency and compensation.
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationRecord {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    /** JSON string: {@code {"productId": qty, ...}} */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String items;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ReservationStatus {
        ACTIVE, CONFIRMED, RELEASED
    }
}
