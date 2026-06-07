package com.hieu.order_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_orders_order_number", columnNames = "order_number"),
                @UniqueConstraint(name = "uq_orders_idempotency_key", columnNames = "idempotency_key")
        })
@Getter @Setter
public class OrderJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "ward", nullable = false)
    private String ward;

    @Column(name = "district", nullable = false)
    private String district;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "shipment_id", length = 36)
    private String shipmentId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;
}
