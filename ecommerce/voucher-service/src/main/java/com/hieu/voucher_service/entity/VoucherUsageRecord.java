package com.hieu.voucher_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Theo dõi per-user voucher usage để enforce usageLimitPerUser.
 * Được tạo khi apply thành công, xóa khi release.
 */
@Entity
@Table(
    name = "voucher_usage_records",
    indexes = {
        @Index(name = "idx_usage_voucher_user", columnList = "voucher_id, user_id"),
        @Index(name = "idx_usage_order", columnList = "order_id")
    }
)
public class VoucherUsageRecord {

    @Id
    @Column(length = 36)
    private String id;

    /** FK-like reference (không dùng @ManyToOne để tránh join cost). */
    @Column(name = "voucher_id", nullable = false, length = 36)
    private String voucherId;

    /** User đã apply voucher. Dùng String để match auth-service (UUID string). */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Order ID — dùng cho idempotent release. */
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Instant usedAt;

    // Required by JPA
    public VoucherUsageRecord() {}

    public VoucherUsageRecord(String voucherId, String userId, String orderId) {
        this.voucherId = voucherId;
        this.userId = userId;
        this.orderId = orderId;
        this.usedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public String getVoucherId() { return voucherId; }
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public Instant getUsedAt() { return usedAt; }
}
