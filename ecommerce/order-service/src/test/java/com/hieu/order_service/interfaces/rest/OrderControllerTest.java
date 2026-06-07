package com.hieu.order_service.interfaces.rest;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.order_service.application.command.order.CancelOrderCommand;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.handler.order.*;
import com.hieu.order_service.application.query.order.GetOrderByIdInternalQuery;
import com.hieu.order_service.application.query.order.GetOrderByIdQuery;
import com.hieu.order_service.application.query.order.GetOrderByNumberQuery;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.OrderJpaEntity;
import com.hieu.order_service.infrastructure.persistence.jpa.repositories.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the branching logic inside {@link OrderController}:
 * admin-flag derivation, the public phone-gated track endpoint, the internal
 * token gate, and customer-stats projection mapping. Handlers + repo are mocked.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock CreateOrderHandler createOrderHandler;
    @Mock CreateOrderFromCartHandler createOrderFromCartHandler;
    @Mock CancelOrderHandler cancelOrderHandler;
    @Mock AdminOrderActionHandler adminOrderActionHandler;
    @Mock GetOrderByIdHandler getOrderByIdHandler;
    @Mock GetOrderByIdInternalHandler getOrderByIdInternalHandler;
    @Mock GetOrderByNumberHandler getOrderByNumberHandler;
    @Mock ListOrdersByUserHandler listOrdersByUserHandler;
    @Mock ListOrdersByStatusHandler listOrdersByStatusHandler;
    @Mock ListOrdersCursorHandler listOrdersCursorHandler;
    @Mock HasUserPurchasedProductHandler hasUserPurchasedProductHandler;
    @Mock OrderJpaRepository orderRepo;

    OrderController controller;

    private static final OrderDTO DTO = sampleDto();

    @BeforeEach
    void setUp() {
        controller = new OrderController(
                createOrderHandler, createOrderFromCartHandler, cancelOrderHandler,
                adminOrderActionHandler, getOrderByIdHandler, getOrderByIdInternalHandler,
                getOrderByNumberHandler, listOrdersByUserHandler, listOrdersByStatusHandler,
                listOrdersCursorHandler, hasUserPurchasedProductHandler, orderRepo);
        ReflectionTestUtils.setField(controller, "internalToken", "secret-123");
    }

    private static OrderDTO sampleDto() {
        return new OrderDTO("00000000-0000-0000-0000-000000000001", "ORD-20240101-000001", "user-1", "CONFIRMED", List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, "R", "0901234567", "s", "w", "d", "c", "Vietnam", null,
                null, "COD", null, null, null, null,
                Instant.now(), Instant.now(), null, null, null, null, null);
    }

    private AuthenticatedUser user(String... roles) {
        return new AuthenticatedUser("user-1", "john", List.of(roles), List.of());
    }

    @Test
    @DisplayName("getById sets isAdmin=true when the principal has ROLE_ADMIN")
    void getById_admin() {
        when(getOrderByIdHandler.handle(any())).thenReturn(DTO);

        controller.getById("00000000-0000-0000-0000-000000000005", user("ROLE_ADMIN"));

        var captor = ArgumentCaptor.forClass(GetOrderByIdQuery.class);
        verify(getOrderByIdHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(5L);
        assertThat(captor.getValue().requestingUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().isAdmin()).isTrue();
    }

    @Test
    @DisplayName("getById sets isAdmin=false for a normal user")
    void getById_nonAdmin() {
        when(getOrderByIdHandler.handle(any())).thenReturn(DTO);

        controller.getById("00000000-0000-0000-0000-000000000005", user("ROLE_USER"));

        var captor = ArgumentCaptor.forClass(GetOrderByIdQuery.class);
        verify(getOrderByIdHandler).handle(captor.capture());
        assertThat(captor.getValue().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("getByNumber forwards the admin flag")
    void getByNumber_admin() {
        when(getOrderByNumberHandler.handle(any())).thenReturn(DTO);

        controller.getByNumber("ORD-20240101-000001", user("ROLE_ADMIN"));

        var captor = ArgumentCaptor.forClass(GetOrderByNumberQuery.class);
        verify(getOrderByNumberHandler).handle(captor.capture());
        assertThat(captor.getValue().isAdmin()).isTrue();
    }

    @Test
    @DisplayName("cancelOrder reads the reason from the body and forwards the admin flag")
    void cancelOrder_forwardsReasonAndAdmin() {
        when(cancelOrderHandler.handle(any())).thenReturn(DTO);

        controller.cancelOrder("00000000-0000-0000-0000-000000000009", Map.of("reason", "changed mind"), user("ROLE_USER"));

        var captor = ArgumentCaptor.forClass(CancelOrderCommand.class);
        verify(cancelOrderHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(9L);
        assertThat(captor.getValue().reason()).isEqualTo("changed mind");
        assertThat(captor.getValue().requestingUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("track rejects a blank phone with 403")
    void track_blankPhone() {
        assertThatThrownBy(() -> controller.track("ORD-20240101-000001", "  "))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(orderRepo);
    }

    @Test
    @DisplayName("track throws OrderNotFound for an unknown order number")
    void track_notFound() {
        when(orderRepo.findByOrderNumber("ORD-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.track("ORD-X", "0901234567"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("track rejects a phone that does not match the stored recipient phone")
    void track_phoneMismatch() {
        var raw = new OrderJpaEntity();
        raw.setRecipientPhone("0900000000");
        raw.setUserId("user-1");
        when(orderRepo.findByOrderNumber("ORD-1")).thenReturn(Optional.of(raw));

        assertThatThrownBy(() -> controller.track("ORD-1", "0901234567"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(getOrderByNumberHandler);
    }

    @Test
    @DisplayName("track with a matching (trimmed) phone delegates to the handler with admin bypass")
    void track_matchingPhone() {
        var raw = new OrderJpaEntity();
        raw.setRecipientPhone("0901234567");
        raw.setUserId("owner-99");
        when(orderRepo.findByOrderNumber("ORD-1")).thenReturn(Optional.of(raw));
        when(getOrderByNumberHandler.handle(any())).thenReturn(DTO);

        var result = controller.track("ORD-1", " 0901234567 ");

        assertThat(result).isSameAs(DTO);
        var captor = ArgumentCaptor.forClass(GetOrderByNumberQuery.class);
        verify(getOrderByNumberHandler).handle(captor.capture());
        assertThat(captor.getValue().orderNumber()).isEqualTo("ORD-1");
        assertThat(captor.getValue().requestingUserId()).isEqualTo("owner-99");
        assertThat(captor.getValue().isAdmin()).isTrue();
    }

    @Test
    @DisplayName("getInternal rejects a wrong X-Internal-Token")
    void getInternal_wrongToken() {
        assertThatThrownBy(() -> controller.getInternal("00000000-0000-0000-0000-000000000001", "nope"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(getOrderByIdInternalHandler);
    }

    @Test
    @DisplayName("getInternal rejects a null token")
    void getInternal_nullToken() {
        assertThatThrownBy(() -> controller.getInternal("00000000-0000-0000-0000-000000000001", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getInternal with the correct token delegates to the internal handler")
    void getInternal_correctToken() {
        when(getOrderByIdInternalHandler.handle(any())).thenReturn(DTO);

        var result = controller.getInternal("00000000-0000-0000-0000-000000000001", "secret-123");

        assertThat(result).isSameAs(DTO);
        var captor = ArgumentCaptor.forClass(GetOrderByIdInternalQuery.class);
        verify(getOrderByIdInternalHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getInternal denies access when the configured internal token is blank")
    void getInternal_blankConfigured() {
        ReflectionTestUtils.setField(controller, "internalToken", "");

        assertThatThrownBy(() -> controller.getInternal("00000000-0000-0000-0000-000000000001", "anything"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("customerStats maps each projection row into an ordered map")
    void customerStats_mapsRows() {
        var row = mock(OrderJpaRepository.CustomerStatsView.class);
        when(row.getUserId()).thenReturn("user-1");
        when(row.getOrderCount()).thenReturn(3L);
        when(row.getLifetimeValue()).thenReturn(new BigDecimal("999.00"));
        var when = Instant.parse("2024-01-01T00:00:00Z");
        when(row.getLastOrderAt()).thenReturn(when);
        when(orderRepo.aggregateByUser(null)).thenReturn(List.of(row));

        var result = controller.customerStats(null);

        assertThat(result).hasSize(1);
        var m = result.get(0);
        assertThat(m.get("userId")).isEqualTo("user-1");
        assertThat(m.get("orderCount")).isEqualTo(3L);
        assertThat(m.get("lifetimeValue")).isEqualTo(new BigDecimal("999.00"));
        assertThat(m.get("lastOrderAt")).isEqualTo(when);
    }

    @Test
    @DisplayName("customerStats passes the userIds filter through when non-empty")
    void customerStats_withUserIds() {
        when(orderRepo.aggregateByUser(List.of("a", "b"))).thenReturn(List.of());

        controller.customerStats(List.of("a", "b"));

        verify(orderRepo).aggregateByUser(List.of("a", "b"));
    }
}
