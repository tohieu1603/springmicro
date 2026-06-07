package com.hieu.voucher_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "vouchers",
    uniqueConstraints = @UniqueConstraint(columnNames = "code"),
    indexes = {
        @Index(name = "idx_voucher_active", columnList = "active"),
        @Index(name = "idx_voucher_end_date", columnList = "end_date")
    }
)
@Getter
@Setter
public class VoucherJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    /** PERCENTAGE hoặc FIXED_AMOUNT */
    @Column(nullable = false, length = 20)
    private String type;

    /** Giá trị giảm: 10 = 10% (PERCENTAGE) hoặc 50000đ (FIXED_AMOUNT) */
    @Column(nullable = false)
    private BigDecimal discountValue;

    /** Đơn hàng tối thiểu để áp dụng voucher */
    private BigDecimal minOrderAmount;

    /** Giảm giá tối đa (chỉ dùng khi type=PERCENTAGE) */
    private BigDecimal maxDiscountAmount;

    /** Số lần sử dụng tối đa (null = không giới hạn) */
    private Integer usageLimit;

    /** Số lần mỗi user được dùng (null = không giới hạn) */
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(nullable = false)
    private int usedCount = 0;

    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(nullable = false)
    private boolean active = true;

    /** ALL | NEW_USER | SPECIFIC */
    @Column(name = "target_user_type", length = 20)
    private String targetUserType = "ALL";

    /** userId được áp dụng khi targetUserType=SPECIFIC, lưu dạng CSV: "uuid1,uuid2" */
    @Column(name = "target_user_ids", columnDefinition = "TEXT")
    private String targetUserIds;

    /** productId được áp dụng (null = tất cả), lưu CSV */
    @Column(name = "applicable_product_ids", columnDefinition = "TEXT")
    private String applicableProductIds;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // @Version removed — pessimistic lock (PESSIMISTIC_WRITE) is sufficient; @Version causes spurious OptimisticLockException

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
