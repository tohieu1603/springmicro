package com.hieu.voucher_service.kafka;

import com.hieu.voucher_service.AbstractIntegrationTest;
import com.hieu.voucher_service.dto.CreateVoucherRequest;
import com.hieu.voucher_service.repository.VoucherJpaRepository;
import com.hieu.voucher_service.repository.VoucherUsageRecordRepository;
import com.hieu.voucher_service.service.VoucherApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderEventConsumer (voucher-service) — IT")
class OrderEventConsumerIT extends AbstractIntegrationTest {

    @Autowired VoucherApplicationService    voucherService;
    @Autowired VoucherJpaRepository         voucherRepo;
    @Autowired VoucherUsageRecordRepository usageRecordRepo;
    @Autowired OrderEventConsumer           consumer;

    @BeforeEach
    void cleanDb() {
        usageRecordRepo.deleteAll();
        voucherRepo.deleteAll();
    }

    // ── Fixture ───────────────────────────────────────────────────────────────────

    void createAndApplyVoucher(String code, String userId, String orderId) {
        var req = CreateVoucherRequest.builder()
                .code(code)
                .type("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(50_000))
                .usageLimit(100)
                .endDate(Instant.now().plusSeconds(3600))
                .build();
        voucherService.createVoucher(req);
        voucherService.validateAndApply(
                code, BigDecimal.valueOf(200_000), userId, orderId, List.of());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("order.cancelled với voucherCode → consumer release voucher → usedCount decrements")
    void orderCancelled_withVoucherCode_releasesVoucher() {
        String code    = "CANCEL-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        String userId  = UUID.randomUUID().toString();
        String orderId = "order-kafka-1";

        createAndApplyVoucher(code, userId, orderId);

        // Verify initial state
        assertThat(voucherRepo.findByCode(code).orElseThrow().getUsedCount()).isEqualTo(1);

        // Simulate consumer receiving order.cancelled event
        var payload = Map.<String, Object>of(
                "orderId",     orderId,
                "voucherCode", code
        );
        consumer.onOrderCancelled(payload);

        // usedCount must be 0 after release
        var entity = voucherRepo.findByCode(code).orElseThrow();
        assertThat(entity.getUsedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("order.cancelled tanpa voucherCode → consumer bỏ qua (không throw)")
    void orderCancelled_withoutVoucherCode_skipsGracefully() {
        var payload = Map.<String, Object>of("orderId", "order-no-voucher");

        assertThatCode(() -> consumer.onOrderCancelled(payload))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("order.cancelled với voucherCode không tồn tại → consumer log error, không propagate")
    void orderCancelled_unknownVoucherCode_doesNotPropagateException() {
        var payload = Map.<String, Object>of(
                "orderId",     "order-xyz",
                "voucherCode", "NON-EXISTENT-VOUCHER"
        );

        // Consumer swallows exceptions for business-logic errors (see implementation)
        assertThatCode(() -> consumer.onOrderCancelled(payload))
                .doesNotThrowAnyException();
    }
}
