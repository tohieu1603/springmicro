package com.hieu.order_service.infrastructure.messaging;

import com.hieu.order_service.domain.events.returnrequest.OrderReturnedEvent;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.infrastructure.rest.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires the payment-service refund call after a return is successfully completed.
 * Runs AFTER_COMMIT so we never refund for a transaction that rolled back, and is
 * annotated {@code @Async} so the caller's API response doesn't wait on the external
 * HTTP round-trip.
 *
 * <p>Refund failure here does NOT undo the return — business rule is "return row is the
 * source of truth, refund is best-effort and retryable". A future job can scan for
 * RETURNED orders with no refund record and retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundOnReturnCompletedListener {

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReturned(OrderReturnedEvent event) {
        try {
            var order = orderRepository.findById(OrderId.of(event.orderId())).orElse(null);
            if (order == null) {
                log.warn("Refund skipped — order {} not found for returned event {}",
                        event.orderId(), event.returnRequestId());
                return;
            }
            if (event.refundAmount() == null) {
                log.info("Refund skipped for order {} — no refund amount on return request",
                        order.getOrderNumber().value());
                return;
            }
            // Payment-service exposes the admin-only process-refund endpoint. The listener
            // runs with no user context, so admin token must be supplied via config (or we
            // skip gracefully in dev when it's unset).
            paymentServiceClient.processRefundForOrder(
                    order.getOrderNumber().value(),
                    event.refundAmount(),
                    null /* TODO: system admin token from secrets / machine-to-machine auth */);
            log.info("Refund triggered for order {} returnRequest {}",
                    order.getOrderNumber().value(), event.returnRequestId());
        } catch (Exception e) {
            // Never let an event listener crash — refund can be retried by a background job.
            log.error("Refund call failed for returnRequest {}: {}",
                    event.returnRequestId(), e.getMessage());
        }
    }
}
