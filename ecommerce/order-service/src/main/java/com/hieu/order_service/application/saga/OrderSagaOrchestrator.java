package com.hieu.order_service.application.saga;

import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.valueobject.Money;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.infrastructure.grpc.client.CatalogGrpcClient;
import com.hieu.order_service.infrastructure.grpc.client.InventoryGrpcClient;
import com.hieu.order_service.infrastructure.rest.client.PaymentServiceClient;
import com.hieu.order_service.infrastructure.rest.client.VoucherServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Order creation saga — price verify → reserve stock → transition state → initiate
 * payment. The outer {@link #executeCreateOrderSaga} is NOT transactional so we never
 * hold a DB connection across external I/O; each state change commits independently via
 * {@link OrderStateTransitioner} (REQUIRES_NEW + AFTER_COMMIT event publishing).
 *
 * <p>Parallelism: step 1 fans out one catalog lookup per item via
 * {@link #sagaExecutor} and joins with {@code CompletableFuture.allOf} — eliminates
 * round-trip stackup on multi-item orders. Subsequent steps are sequential because they
 * mutate the aggregate.
 *
 * <p>Race-condition safety: {@code CreateOrderCommandHandler} persists the order in
 * PENDING under a DB-unique idempotency key BEFORE this saga runs, so duplicate POSTs
 * short-circuit. Inventory's reservation is itself idempotent on {@code orderNumber};
 * payment uses the same key. Cart clear is deferred to the {@code payment.completed}
 * Kafka listener — a failed payment doesn't strand an empty cart.
 */
@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);
    private static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");

    private final OrderRepository orderRepository;
    private final CatalogGrpcClient catalogGrpcClient;
    private final InventoryGrpcClient inventoryGrpcClient;
    private final PaymentServiceClient paymentServiceClient;
    private final VoucherServiceClient voucherServiceClient;
    private final OrderStateTransitioner stateTransitioner;
    private final OrderDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;
    private final Executor sagaExecutor;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                  CatalogGrpcClient catalogGrpcClient,
                                  InventoryGrpcClient inventoryGrpcClient,
                                  PaymentServiceClient paymentServiceClient,
                                  VoucherServiceClient voucherServiceClient,
                                  OrderStateTransitioner stateTransitioner,
                                  OrderDtoMapper mapper,
                                  DomainEventPublisher eventPublisher,
                                  @Qualifier("sagaExecutor") Executor sagaExecutor) {
        this.orderRepository = orderRepository;
        this.catalogGrpcClient = catalogGrpcClient;
        this.inventoryGrpcClient = inventoryGrpcClient;
        this.paymentServiceClient = paymentServiceClient;
        this.voucherServiceClient = voucherServiceClient;
        this.stateTransitioner = stateTransitioner;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.sagaExecutor = sagaExecutor;
    }

    public OrderDTO executeCreateOrderSaga(String orderId, String authToken) {
        // Single load — the JPA adapter does LEFT JOIN FETCH on items so iterating
        // order.getItems() later doesn't trigger a per-row lazy query.
        var order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        var orderNumber = order.getOrderNumber().value();

        boolean voucherApplied = false;
        try {
            // Step 1 — parallel catalog price verification.
            verifyPricesInParallel(order);

            // Step 1.5 — validate + apply voucher BEFORE reserving stock so a rejected
            // voucher (404 / 422 / limit) bails out without holding inventory. Successful
            // validate atomically increments voucher.usedCount on the voucher-service side.
            if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
                var productIds = order.getItems().stream()
                        .map(i -> i.getProductId().value())
                        .toList();
                var discount = voucherServiceClient.validateAndApply(
                        order.getVoucherCode(),
                        order.getSubtotalAmount().amount(),
                        order.getUserId().value(),
                        orderId,
                        productIds,
                        authToken);

                stateTransitioner.applyVoucherDiscount(orderId, discount);
                voucherApplied = true;
                // Reload so subsequent steps see the new totalAmount used by payment-service.
                order = orderRepository.findById(OrderId.of(orderId))
                        .orElseThrow(() -> new OrderNotFoundException(orderId));
                log.info("Voucher {} applied to order {}: discount={}",
                        order.getVoucherCode(), orderNumber, discount);
            }

            // Step 2 — reserve stock (idempotent server-side on orderNumber).
            var reservationId = inventoryGrpcClient.reserveStock(
                    orderNumber,
                    order.getItems().stream()
                            .map(i -> new InventoryGrpcClient.ReserveItemInput(
                                    i.getVariantId(), i.getQuantity().value()))
                            .toList());

            // Step 3 — commit state transition (proxy-wrapped REQUIRES_NEW).
            stateTransitioner.markInventoryReservedAndPaymentPending(orderId, reservationId);

            // Step 4 — initiate payment (sync REST, bounded timeout). Propagate user's
            // JWT so payment-service's JWT filter accepts the call.
            var paymentResp = paymentServiceClient.initiate(
                    orderNumber, order.getTotalAmount().amount(),
                    order.getPaymentMethod(), orderNumber, authToken);

            // Step 5 — persist paymentId if returned.
            if (paymentResp.paymentId() != null) {
                stateTransitioner.markPaymentInitiated(orderId, paymentResp.paymentId());
            }

            // Step 6 — return hydrated DTO (cart clear deferred to payment.completed listener).
            var finalOrder = orderRepository.findById(OrderId.of(orderId))
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            return mapper.toDto(finalOrder, paymentResp.payUrl(), paymentResp.qrCodeUrl());

        } catch (Exception e) {
            log.error("Saga failed for order {} ({}): {}", orderId, orderNumber, e.getMessage());
            // Each compensation step is isolated — a failure in one MUST NOT skip the
            // others. Voucher release is best-effort REST; voucher-service also listens
            // to order.cancelled via Kafka as backup.
            try {
                stateTransitioner.markFailedAndReleaseStock(orderId, e.getMessage());
            } catch (Exception ce) {
                log.error("Compensation: markFailedAndReleaseStock failed for order {}: {}", orderId, ce.getMessage(), ce);
            }
            if (voucherApplied) {
                try {
                    voucherServiceClient.release(order.getVoucherCode(), orderId);
                } catch (Exception ve) {
                    log.warn("Compensation: voucher release failed for order {} (Kafka backup will handle): {}",
                            orderId, ve.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * Fan out one catalog gRPC call per item; bail fast on any failure. Running on
     * {@code sagaExecutor} keeps these off the request thread so the Tomcat thread pool
     * isn't blocked while catalog replies.
     */
    private void verifyPricesInParallel(Order order) {
        var futures = order.getItems().stream()
                .map(item -> CompletableFuture
                        .supplyAsync(() -> catalogGrpcClient.getVariantBySku(item.getVariantSku()), sagaExecutor)
                        .thenApply(variantOpt -> {
                            var v = variantOpt.orElseThrow(() ->
                                    new ServiceUnavailableException("variant not found: " + item.getVariantSku()));
                            if (!item.getUnitPrice().isWithin(Money.of(v.price()), PRICE_TOLERANCE)) {
                                throw new ServiceUnavailableException(
                                        "price changed for SKU " + item.getVariantSku());
                            }
                            return true;
                        }))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException ce) {
            // Unwrap so the outer catch sees the real cause (ServiceUnavailable / transport).
            if (ce.getCause() instanceof RuntimeException re) throw re;
            throw ce;
        }
    }

    /**
     * User-initiated cancel flow. Releases the inventory reservation + marks the order
     * CANCELLED. Runs in a single REQUIRES_NEW transaction with a pessimistic lock so
     * concurrent "cancel" clicks don't produce divergent states.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDTO executeCancelOrderSaga(String orderId, String reason, String userId, boolean isAdmin) {
        var order = orderRepository.findByIdWithLock(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        // Owner-or-admin gate. Without this, any authenticated user can cancel any order.
        if (!isAdmin && !order.getUserId().value().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User not allowed to cancel order " + orderId);
        }
        if (order.getReservationId() != null) {
            try {
                inventoryGrpcClient.releaseStock(order.getReservationId().value());
            } catch (Exception e) {
                log.warn("Release stock failed during cancel of order {}: {}", orderId, e.getMessage());
            }
        }
        order.cancel(reason);
        var saved = orderRepository.save(order);
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
