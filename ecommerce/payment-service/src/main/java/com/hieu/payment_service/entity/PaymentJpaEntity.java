package com.hieu.payment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "ix_payments_user_id",  columnList = "user_id"),
        @Index(name = "ix_payments_order_id", columnList = "order_id"),
        @Index(name = "ix_payments_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "order_id", length = 64, nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 8, nullable = false)
    private String currency = "VND";

    @Column(length = 32, nullable = false)
    private String method;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(name = "transaction_id", length = 128)
    private String transactionId;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "qr_code_url", length = 1024)
    private String qrCodeUrl;

    @Column(name = "pay_url", length = 1024)
    private String payUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refund_amount", precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
