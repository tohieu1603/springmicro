package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.domain.exception.InvalidOrderStateException;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link AdminOrderActionHandler}: per-action state
 * transitions, the COD PAYMENT_PENDING→PAYMENT_COMPLETED promotion before
 * CONFIRM, the missing-shipmentId guard, and not-found handling. The aggregate
 * runs its real state machine; only repo/mapper/publisher are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderActionHandler")
class AdminOrderActionHandlerTest {

    private static final String USER       = "11111111-1111-1111-1111-111111111111";
    private static final String ORDER_ID   = "00000000-0000-0000-0000-000000000001";
    private static final String PAYMENT_ID = "00000000-0000-0000-0000-000000000005";
    private static final String SHIPMENT_ID = "00000000-0000-0000-0000-000000000077";

    @Mock OrderRepository orderRepository;
    @Mock OrderDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks AdminOrderActionHandler handler;

    private final OrderDTO dto = mock(OrderDTO.class);

    static Order orderInStatus(OrderStatus status) {
        return Order.reconstitute(
                OrderId.of(ORDER_ID), OrderNumber.of("ORD-20260101-000001"), UserId.of(USER),
                status, Money.of(BigDecimal.valueOf(100_000)), Money.ZERO, Money.ZERO,
                Money.of(BigDecimal.valueOf(100_000)), null,
                RecipientName.of("Nguyen Van A"), RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000"),
                null, "COD", PAYMENT_ID, ReservationId.of("res-1"), null, "idem-1", null,
                Instant.now(), Instant.now(), null, null, null, USER, USER, 0L);
    }

    @Test
    @DisplayName("order not found → OrderNotFoundException")
    void notFound() {
        when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.apply(ORDER_ID, AdminOrderActionHandler.Action.DELIVER, null))
                .isInstanceOf(OrderNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Nested
    @DisplayName("CONFIRM")
    class Confirm {

        @Test
        @DisplayName("COD order at PAYMENT_PENDING → promotes to PAYMENT_COMPLETED then CONFIRMED")
        void codPaymentPending_promotedThenConfirmed() {
            var order = orderInStatus(OrderStatus.PAYMENT_PENDING);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(mapper.toDto(order)).thenReturn(dto);

            var result = handler.apply(ORDER_ID, AdminOrderActionHandler.Action.CONFIRM, null);

            assertThat(result).isSameAs(dto);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(eventPublisher).publishEventsOf(order);
        }

        @Test
        @DisplayName("order at PAYMENT_COMPLETED → confirm() directly")
        void paymentCompleted_confirmed() {
            var order = orderInStatus(OrderStatus.PAYMENT_COMPLETED);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(mapper.toDto(order)).thenReturn(dto);

            handler.apply(ORDER_ID, AdminOrderActionHandler.Action.CONFIRM, null);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("CONFIRM from illegal state (PENDING) → InvalidOrderStateException")
        void confirmFromPending_throws() {
            var order = orderInStatus(OrderStatus.PENDING);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> handler.apply(ORDER_ID, AdminOrderActionHandler.Action.CONFIRM, null))
                    .isInstanceOf(InvalidOrderStateException.class);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("SHIP")
    class Ship {

        @Test
        @DisplayName("null shipmentId → InvalidOrderStateException, never saves")
        void nullShipmentId_throws() {
            var order = orderInStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> handler.apply(ORDER_ID, AdminOrderActionHandler.Action.SHIP, null))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("shipmentId");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("CONFIRMED + shipmentId → SHIPPED with shipmentId set")
        void confirmed_shipsWithId() {
            var order = orderInStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(mapper.toDto(order)).thenReturn(dto);

            handler.apply(ORDER_ID, AdminOrderActionHandler.Action.SHIP, SHIPMENT_ID);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(order.getShipmentId()).isEqualTo(SHIPMENT_ID);
            verify(eventPublisher).publishEventsOf(order);
        }
    }

    @Nested
    @DisplayName("DELIVER")
    class Deliver {

        @Test
        @DisplayName("SHIPPED → DELIVERED, deliveredAt set")
        void shipped_delivered() {
            var order = orderInStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(mapper.toDto(order)).thenReturn(dto);

            handler.apply(ORDER_ID, AdminOrderActionHandler.Action.DELIVER, null);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(order.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("DELIVER from CONFIRMED (never shipped) → InvalidOrderStateException")
        void deliverWithoutShip_throws() {
            var order = orderInStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> handler.apply(ORDER_ID, AdminOrderActionHandler.Action.DELIVER, null))
                    .isInstanceOf(InvalidOrderStateException.class);
        }
    }
}
