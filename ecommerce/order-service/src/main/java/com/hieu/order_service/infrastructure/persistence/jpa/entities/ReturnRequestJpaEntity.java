package com.hieu.order_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Getter @Setter
public class ReturnRequestJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "return_type", nullable = false, length = 20)
    private String returnType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "refund_amount", precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
