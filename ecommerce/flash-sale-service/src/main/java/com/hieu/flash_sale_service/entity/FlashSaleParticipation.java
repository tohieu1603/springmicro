package com.hieu.flash_sale_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Records each user's participation (quantity claimed) in a flash sale. */
@Entity
@Table(
    name = "flash_sale_participations",
    indexes = {
        @Index(name = "ix_participation_sale_user", columnList = "sale_id, user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FlashSaleParticipation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "sale_id", nullable = false, length = 36)
    private String saleId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "participated_at", nullable = false, updatable = false)
    private Instant participatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = java.util.UUID.randomUUID().toString();
        this.participatedAt = Instant.now();
    }
}
