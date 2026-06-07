package com.hieu.voucher_service.repository;

import com.hieu.voucher_service.entity.VoucherUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherUsageRecordRepository extends JpaRepository<VoucherUsageRecord, String> {

    /** Đếm số lần user đã dùng voucher — enforce usageLimitPerUser. */
    long countByVoucherIdAndUserId(String voucherId, String userId);

    /** Tìm record theo orderId — dùng cho idempotent release. */
    Optional<VoucherUsageRecord> findByOrderId(String orderId);

    /** Xóa record khi release voucher. */
    void deleteByOrderId(String orderId);
}
