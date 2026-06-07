package com.hieu.order_service.application.saga;

import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.Money;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.model.order.valueobject.OrderStatus;
import com.hieu.order_service.domain.model.order.valueobject.ReservationId;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.infrastructure.grpc.client.InventoryGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * State transitions extracted from {@link OrderSagaOrchestrator} into a separate bean
 * so Spring's transactional proxy actually wraps them.
 *
 * <p>When the saga called these methods via {@code this.X()} they were proxy-bypassed
 * and the {@code @Transactional(REQUIRES_NEW)} annotation did nothing — every update
 * silently shared the saga's outer (non-existent) transaction. Moving them here forces
 * every call to cross a proxy boundary, so each state change commits independently as
 * the saga pattern intends.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStateTransitioner {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final InventoryGrpcClient inventoryGrpcClient;

    /**
     * Persist the voucher discount on the order. Recalculates totalAmount = subtotal -
     * discount + shipping. Called BEFORE inventory reservation so a voucher rejection
     * skips the rest of the saga without holding stock.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyVoucherDiscount(String orderId, BigDecimal discountAmount) {
        var order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.applyDiscount(Money.of(discountAmount));
        orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markInventoryReservedAndPaymentPending(String orderId, String reservationId) {
        var order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markInventoryReserved(ReservationId.of(reservationId));
        order.markPaymentPending();
        eventPublisher.publishEventsOf(orderRepository.save(order));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentInitiated(String orderId, String paymentId) {
        var order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markPaymentInitiated(paymentId);
        eventPublisher.publishEventsOf(orderRepository.save(order));
    }

    /**
     * Compensation path — release the upstream stock reservation + flag the order as
     * FAILED. Swallows inventory-service errors so the DB transition still commits
     * (inventory has its own idempotent retry semantics).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedAndReleaseStock(String orderId, String reason) {
        var order = orderRepository.findById(OrderId.of(orderId)).orElse(null);
        if (order == null) return;
        if (order.getReservationId() != null) {
            try {
                inventoryGrpcClient.releaseStock(order.getReservationId().value());
            } catch (Exception e) {
                log.warn("Release stock failed for reservation {}: {}",
                        order.getReservationId().value(), e.getMessage());
            }
        }
        if (order.getStatus().canTransitionTo(OrderStatus.FAILED)) {
            order.markFailed(reason);
            eventPublisher.publishEventsOf(orderRepository.save(order));
        }
    }
}
