package com.hieu.order_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
@Getter @Setter
public class IdempotencyJpaEntity {

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;
}
