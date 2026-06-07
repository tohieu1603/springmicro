package com.hieu.auth_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_user_id", columnList = "user_id"),
        @Index(name = "idx_rt_family", columnList = "family"),
        @Index(name = "idx_rt_revoked_expiry", columnList = "revoked,expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenJpaEntity extends BaseManualIdEntity {

    @Column(unique = true, nullable = false, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "family", nullable = false, length = 100)
    private String family;

    private Integer generation = 0;

    @Column(name = "revoked_reason", length = 50)
    private String revokedReason;

    @Builder
    public RefreshTokenJpaEntity(String id, String token, UserJpaEntity user,
                                 Instant expiryDate, Boolean revoked, Instant createdAt,
                                 Instant revokedAt, String family, Integer generation,
                                 String revokedReason, boolean isNew) {
        this.id = id;
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.revoked = revoked != null ? revoked : false;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.family = family;
        this.generation = generation != null ? generation : 0;
        this.revokedReason = revokedReason;
        this.isNew = isNew;
    }
}