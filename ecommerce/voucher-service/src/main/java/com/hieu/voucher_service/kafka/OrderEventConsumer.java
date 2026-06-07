package com.hieu.voucher_service.kafka;

import com.hieu.voucher_service.service.VoucherApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lắng nghe order.cancelled — nếu order có voucherCode thì release voucher (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final VoucherApplicationService voucherService;

    @KafkaListener(topics = "order.cancelled", groupId = "voucher-service")
    public void onOrderCancelled(Map<String, Object> payload) {
        try {
            Object voucherCode = payload.get("voucherCode");
            // Saga passes numeric orderId (Long) into /validate; release must match the
            // same key shape — voucher_usage_records.order_id stores it as String.
            Object orderId = payload.get("orderId");

            if (voucherCode == null || orderId == null) {
                log.debug("order.cancelled event has no voucherCode/orderId — skipping");
                return;
            }

            String code = voucherCode.toString();
            String oid = orderId.toString();

            if (code.isBlank() || oid.isBlank()) {
                log.debug("order.cancelled event has blank voucherCode/orderId — skipping");
                return;
            }

            log.info("Releasing voucher {} for cancelled order {}", code, oid);
            voucherService.releaseVoucher(code, oid);
        } catch (Exception e) {
            // Log và không throw — Kafka sẽ không retry vô hạn cho lỗi business logic
            log.error("Error processing order.cancelled event: payload={}, error={}", payload, e.getMessage(), e);
        }
    }
}
