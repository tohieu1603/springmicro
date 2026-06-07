package com.hieu.shipping_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for {@code shipments} table.
 */
@Entity
@Table(
    name = "shipments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_shipment_order_id", columnNames = "order_id"),
        @UniqueConstraint(name = "uq_shipment_tracking_number", columnNames = "tracking_number")
    },
    indexes = {
        @Index(name = "idx_shipment_user_id",         columnList = "user_id"),
        @Index(name = "idx_shipment_status",           columnList = "status"),
        @Index(name = "idx_shipment_tracking_number",  columnList = "tracking_number"),
        @Index(name = "idx_shipment_order_id",         columnList = "order_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ShipmentJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "carrier", length = 32)
    private String carrier;

    @Column(name = "tracking_number", unique = true, length = 64)
    private String trackingNumber;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "recipient_name", nullable = false, length = 128)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "address_line", nullable = false, columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "ward", length = 128)
    private String ward;

    @Column(name = "district", length = 128)
    private String district;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "country", nullable = false, length = 64)
    private String country = "Vietnam";

    @Column(name = "estimated_delivery_date")
    private Instant estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private Instant actualDeliveryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.country == null) this.country = "Vietnam";
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
