package com.hieu.voucher_service.service;

import com.hieu.voucher_service.AbstractIntegrationTest;
import com.hieu.voucher_service.dto.CreateVoucherRequest;
import com.hieu.voucher_service.dto.VoucherDTO;
import com.hieu.voucher_service.repository.VoucherJpaRepository;
import com.hieu.voucher_service.repository.VoucherUsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("VoucherApplicationService — IT")
class VoucherApplicationServiceIT extends AbstractIntegrationTest {

    @Autowired VoucherApplicationService voucherService;
    @Autowired VoucherJpaRepository      voucherRepo;
    @Autowired VoucherUsageRecordRepository usageRecordRepo;

    @BeforeEach
    void cleanDb() {
        usageRecordRepo.deleteAll();
        voucherRepo.deleteAll();
    }

    // ── Fixture ──────────────────────────────────────────────────────────────────

    VoucherDTO createVoucher(String code, int usageLimit, int initialUsed) {
        var req = CreateVoucherRequest.builder()
                .code(code)
                .type("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(10))
                .usageLimit(usageLimit)
                .endDate(Instant.now().plusSeconds(3600))
                .build();
        var dto = voucherService.createVoucher(req);
        // Manually set usedCount to initialUsed via repo for concurrency tests
        if (initialUsed > 0) {
            var entity = voucherRepo.findByCode(code).orElseThrow();
            entity.setUsedCount(initialUsed);
            voucherRepo.save(entity);
        }
        return dto;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ValidateAndApply")
    class ValidateAndApply {

        @Test
        @DisplayName("happy path: apply thành công → usedCount tăng lên 1")
        void apply_happyPath_incrementsUsedCount() {
            createVoucher("APPLY10", 100, 0);
            String userId = UUID.randomUUID().toString();

            voucherService.validateAndApply(
                    "APPLY10", BigDecimal.valueOf(500_000), userId, "order-1", List.of());

            var entity = voucherRepo.findByCode("APPLY10").orElseThrow();
            assertThat(entity.getUsedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("discount đúng: 10% trên 300,000 → discountAmount = 30,000")
        void apply_discountAmountIsCorrect() {
            createVoucher("DISC10", 100, 0);
            String userId = UUID.randomUUID().toString();

            var resp = voucherService.validateAndApply(
                    "DISC10", BigDecimal.valueOf(300_000), userId, "order-2", List.of());

            assertThat(resp.getDiscountAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(30_000));
        }
    }

    @Nested
    @DisplayName("Release")
    class Release {

        @Test
        @DisplayName("release sau khi apply → usedCount giảm về 0")
        void release_afterApply_decrementsCount() {
            createVoucher("REL10", 100, 0);
            String userId = UUID.randomUUID().toString();

            voucherService.validateAndApply(
                    "REL10", BigDecimal.valueOf(200_000), userId, "order-rel-1", List.of());

            voucherService.releaseVoucher("REL10", "order-rel-1");

            var entity = voucherRepo.findByCode("REL10").orElseThrow();
            assertThat(entity.getUsedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("release gọi 2 lần (idempotent) → count không xuống dưới 0")
        void release_calledTwice_idempotent() {
            createVoucher("REL-IDEM", 100, 0);
            String userId = UUID.randomUUID().toString();

            voucherService.validateAndApply(
                    "REL-IDEM", BigDecimal.valueOf(200_000), userId, "order-idem-1", List.of());

            voucherService.releaseVoucher("REL-IDEM", "order-idem-1");  // 1st call
            voucherService.releaseVoucher("REL-IDEM", "order-idem-1");  // 2nd call — idempotent

            var entity = voucherRepo.findByCode("REL-IDEM").orElseThrow();
            assertThat(entity.getUsedCount()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Concurrency (pessimistic lock)")
    class Concurrency {

        /**
         * CRITICAL: 5 threads đồng thời apply cùng 1 voucher (limit=10, used=5).
         * Expect: usedCount cuối cùng = 10, không over-allocation.
         */
        @Test
        @DisplayName("5 concurrent applies (usageLimit=10, used=5) → usedCount=10, không vượt giới hạn")
        void concurrentApply_pessimisticLock_noOverAllocation() throws InterruptedException {
            createVoucher("CONC10", 10, 5);

            int threads = 5;
            var latch = new CountDownLatch(1);
            var readyLatch = new CountDownLatch(threads);
            var successCount = new AtomicInteger(0);
            var failCount = new AtomicInteger(0);
            var errors = new ArrayList<String>();

            try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
                for (int i = 0; i < threads; i++) {
                    final int idx = i;
                    pool.submit(() -> {
                        readyLatch.countDown();
                        try {
                            latch.await(5, TimeUnit.SECONDS);
                            voucherService.validateAndApply(
                                    "CONC10",
                                    BigDecimal.valueOf(500_000),
                                    UUID.randomUUID().toString(),
                                    "order-conc-" + idx,
                                    List.of());
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            synchronized (errors) { errors.add(e.getMessage()); }
                        }
                    });
                }

                readyLatch.await(5, TimeUnit.SECONDS);
                latch.countDown();  // release all threads simultaneously
                pool.shutdown();
                pool.awaitTermination(30, TimeUnit.SECONDS);
            }

            var entity = voucherRepo.findByCode("CONC10").orElseThrow();
            int finalUsedCount = entity.getUsedCount();

            // Must not exceed the limit
            assertThat(finalUsedCount)
                    .as("usedCount must not exceed usageLimit=10")
                    .isLessThanOrEqualTo(10);

            // All 5 slots were available (used=5, limit=10) so all should succeed
            assertThat(successCount.get())
                    .as("all 5 threads should succeed since 5 slots remain")
                    .isEqualTo(5);

            assertThat(finalUsedCount)
                    .as("final usedCount = initial 5 + 5 successful applies")
                    .isEqualTo(10);
        }
    }
}
