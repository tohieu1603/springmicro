package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.command.order.CancelOrderCommand;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.saga.OrderSagaOrchestrator;
import com.hieu.order_service.domain.exception.CancelNotAllowedException;
import com.hieu.order_service.domain.exception.CancelRateLimitedException;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link CancelOrderHandler}'s customer-side guardrails:
 * ownership, status policy, reason length, and the Redis-backed rate limit
 * (with DB fallback). The saga itself is mocked — these tests only verify the
 * pre-saga gating decisions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelOrderHandler")
class CancelOrderHandlerTest {

    private static final String OWNER    = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER    = "22222222-2222-2222-2222-222222222222";
    private static final String ORDER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String PROD_ID  = "11111111-1111-1111-1111-111111111111";

    @Mock OrderRepository orderRepository;
    @Mock OrderSagaOrchestrator saga;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks CancelOrderHandler handler;

    private final OrderDTO sagaResult = mock(OrderDTO.class);

    static Order orderInStatus(OrderStatus status, String ownerId) {
        var o = Order.create(
                UserId.of(ownerId),
                OrderNumber.of("ORD-20260101-000001"),
                RecipientName.of("Nguyen Van A"),
                RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000"),
                "COD", null, null, "idem-1", ownerId);
        o.addItem(OrderItem.create(
                ProductId.of(PROD_ID), ProductName.of("Product A"),
                null, "SKU-001", null,
                Money.of(BigDecimal.valueOf(100_000)), Quantity.of(1)));
        o.assignId(ORDER_ID);
        // Drive the aggregate to the requested status through legal transitions.
        switch (status) {
            case PENDING -> { /* already */ }
            case INVENTORY_RESERVED -> o.markInventoryReserved(ReservationId.of("res-1"));
            case PAYMENT_PENDING -> {
                o.markInventoryReserved(ReservationId.of("res-1"));
                o.markPaymentPending();
            }
            case PAYMENT_FAILED -> {
                o.markInventoryReserved(ReservationId.of("res-1"));
                o.markPaymentPending();
                // transition PAYMENT_PENDING -> PAYMENT_FAILED is not exposed via a
                // mark* method, so reconstitute instead.
                return reconstituteWith(OrderStatus.PAYMENT_FAILED, ownerId);
            }
            case SHIPPED -> {
                return reconstituteWith(OrderStatus.SHIPPED, ownerId);
            }
            case CONFIRMED -> {
                return reconstituteWith(OrderStatus.CONFIRMED, ownerId);
            }
            default -> { return reconstituteWith(status, ownerId); }
        }
        return o;
    }

    static Order reconstituteWith(OrderStatus status, String ownerId) {
        return Order.reconstitute(
                OrderId.of(ORDER_ID), OrderNumber.of("ORD-20260101-000001"), UserId.of(ownerId),
                status, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, null,
                RecipientName.of("Nguyen Van A"), RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000"),
                null, "COD", null, ReservationId.of("res-1"), null, "idem-1", null,
                Instant.now(), Instant.now(), null, null, null, ownerId, ownerId, 0L);
    }

    @Nested
    @DisplayName("not-found / ownership")
    class Lookup {

        @Test
        @DisplayName("missing order → OrderNotFoundException, saga never runs")
        void notFound_throws() {
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, "đổi ý quá nhiều", OWNER, false)))
                    .isInstanceOf(OrderNotFoundException.class);
            verifyNoInteractions(saga);
        }

        @Test
        @DisplayName("non-owner customer → AccessDeniedException (beats policy/rate checks)")
        void nonOwner_throwsAccessDenied() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OTHER, false)))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(saga);
            verifyNoInteractions(redis);
        }
    }

    @Nested
    @DisplayName("customer policy gates")
    class CustomerPolicy {

        @Test
        @DisplayName("status not in cancellable set → CancelNotAllowedException")
        void shippedOrder_notCancellable() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.SHIPPED, OWNER)));

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OWNER, false)))
                    .isInstanceOf(CancelNotAllowedException.class);
            verifyNoInteractions(saga);
        }

        @Test
        @DisplayName("null reason → IllegalArgumentException (< 5 chars)")
        void nullReason_rejected() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, null, OWNER, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 5");
            verifyNoInteractions(saga);
        }

        @Test
        @DisplayName("reason trimmed to < 5 chars → IllegalArgumentException")
        void shortTrimmedReason_rejected() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, "  ab  ", OWNER, false)))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(saga);
        }
    }

    @Nested
    @DisplayName("rate limiting via Redis INCR")
    class RateLimit {

        @Test
        @DisplayName("first cancel of window → INCR returns 1, EXPIRE set, saga runs")
        void firstCancel_setsExpiryAndProceeds() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("cancel-rl:" + OWNER)).thenReturn(1L);
            when(saga.executeCancelOrderSaga(ORDER_ID, "valid reason", OWNER, false)).thenReturn(sagaResult);

            var result = handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OWNER, false));

            assertThat(result).isSameAs(sagaResult);
            verify(redis).expire("cancel-rl:" + OWNER, Duration.ofHours(24));
            verify(saga).executeCancelOrderSaga(ORDER_ID, "valid reason", OWNER, false);
        }

        @Test
        @DisplayName("count within limit (3) → no EXPIRE re-set, saga runs")
        void withinLimit_proceedsWithoutExpire() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("cancel-rl:" + OWNER)).thenReturn(3L);
            when(saga.executeCancelOrderSaga(ORDER_ID, "valid reason", OWNER, false)).thenReturn(sagaResult);

            handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OWNER, false));

            verify(redis, never()).expire(anyString(), any(Duration.class));
            verify(saga).executeCancelOrderSaga(ORDER_ID, "valid reason", OWNER, false);
        }

        @Test
        @DisplayName("count exceeds limit → CancelRateLimitedException, saga never runs")
        void overLimit_rejected() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("cancel-rl:" + OWNER)).thenReturn(4L);

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OWNER, false)))
                    .isInstanceOf(CancelRateLimitedException.class);
            verifyNoInteractions(saga);
        }

        @Test
        @DisplayName("Redis down → DB fallback under limit → saga runs")
        void redisDown_dbFallbackUnderLimit_proceeds() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));
            when(orderRepository.countCancelledByUserSince(eq(UserId.of(OWNER)), any(Instant.class)))
                    .thenReturn(2L);
            when(saga.executeCancelOrderSaga(ORDER_ID, "valid reason", OWNER, false)).thenReturn(sagaResult);

            var result = handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OWNER, false));

            assertThat(result).isSameAs(sagaResult);
            verify(orderRepository).countCancelledByUserSince(eq(UserId.of(OWNER)), any(Instant.class));
        }

        @Test
        @DisplayName("Redis down → DB fallback at/over limit → CancelRateLimitedException")
        void redisDown_dbFallbackOverLimit_rejected() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.PENDING, OWNER)));
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));
            when(orderRepository.countCancelledByUserSince(eq(UserId.of(OWNER)), any(Instant.class)))
                    .thenReturn(3L);

            assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(ORDER_ID, "valid reason", OWNER, false)))
                    .isInstanceOf(CancelRateLimitedException.class);
            verifyNoInteractions(saga);
        }
    }

    @Nested
    @DisplayName("admin bypass")
    class AdminBypass {

        @Test
        @DisplayName("admin cancels SHIPPED order with short/null reason, not owner → no gates, saga runs")
        void admin_bypassesAllCustomerGates() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(orderInStatus(OrderStatus.SHIPPED, OWNER)));
            when(saga.executeCancelOrderSaga(anyString(), any(), anyString(), eq(true))).thenReturn(sagaResult);

            var result = handler.handle(new CancelOrderCommand(ORDER_ID, null, OTHER, true));

            assertThat(result).isSameAs(sagaResult);
            // No ownership / policy / reason / rate-limit checks for admins.
            verifyNoInteractions(redis);
            verify(saga).executeCancelOrderSaga(ORDER_ID, null, OTHER, true);
        }
    }
}
