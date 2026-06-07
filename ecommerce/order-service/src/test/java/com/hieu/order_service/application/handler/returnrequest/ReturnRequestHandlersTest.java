package com.hieu.order_service.application.handler.returnrequest;

import com.hieu.order_service.application.command.returnrequest.ApproveReturnCommand;
import com.hieu.order_service.application.command.returnrequest.CompleteReturnCommand;
import com.hieu.order_service.application.command.returnrequest.RejectReturnCommand;
import com.hieu.order_service.application.command.returnrequest.RequestReturnCommand;
import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.dto.ReturnRequestDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.domain.events.returnrequest.OrderReturnRequestedEvent;
import com.hieu.order_service.domain.exception.InvalidOrderStateException;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.exception.ReturnRequestNotFoundException;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.ReturnRequest;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.domain.repository.ReturnRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for the return-request command handlers: not-found paths,
 * state-machine guards (approve/reject only PENDING, complete only APPROVED),
 * the 7-day return window, and verification that each handler persists, pulls
 * domain events to the publisher, and maps to a DTO.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Return-request handlers")
class ReturnRequestHandlersTest {

    private static final String USER      = "11111111-1111-1111-1111-111111111111";
    private static final String ORDER_ID  = "00000000-0000-0000-0000-000000000001";
    private static final String RR_ID     = "00000000-0000-0000-0000-000000000050";
    private static final String PAYMENT_ID = "00000000-0000-0000-0000-000000000009";
    private static final String SHIPMENT_ID = "00000000-0000-0000-0000-000000000005";

    @Mock OrderRepository orderRepository;
    @Mock ReturnRequestRepository returnRequestRepository;
    @Mock OrderDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;

    private final ReturnRequestDTO dto = mock(ReturnRequestDTO.class);

    static Order deliveredOrder(Instant deliveredAt) {
        return Order.reconstitute(
                OrderId.of(ORDER_ID), OrderNumber.of("ORD-20260101-000001"), UserId.of(USER),
                OrderStatus.DELIVERED, Money.of(BigDecimal.valueOf(100_000)), Money.ZERO, Money.ZERO,
                Money.of(BigDecimal.valueOf(100_000)), null,
                RecipientName.of("Nguyen Van A"), RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000"),
                null, "COD", PAYMENT_ID, ReservationId.of("res-1"), SHIPMENT_ID, "idem-1", null,
                Instant.now().minus(30, ChronoUnit.DAYS), Instant.now(), null, deliveredAt, null,
                USER, USER, 0L);
    }

    static Order pendingOrder() {
        return Order.reconstitute(
                OrderId.of(ORDER_ID), OrderNumber.of("ORD-20260101-000001"), UserId.of(USER),
                OrderStatus.PENDING, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, null,
                RecipientName.of("Nguyen Van A"), RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000"),
                null, "COD", null, null, null, "idem-1", null,
                Instant.now(), Instant.now(), null, null, null, USER, USER, 0L);
    }

    static ReturnRequest pendingReturn() {
        var rr = ReturnRequest.create(OrderId.of(ORDER_ID), UserId.of(USER),
                ReturnReason.of("hỏng hàng"), ReturnType.REFUND, "[]");
        rr.assignId(RR_ID);
        return rr;
    }

    static ReturnRequest approvedReturn() {
        return ReturnRequest.reconstitute(
                ReturnRequestId.of(RR_ID), OrderId.of(ORDER_ID), UserId.of(USER),
                ReturnReason.of("hỏng hàng"), ReturnType.REFUND, ReturnStatus.APPROVED,
                null, "ok", "[]", Instant.now(), Instant.now());
    }

    // ── RequestReturnHandler ─────────────────────────────────────────────────

    @Nested
    @DisplayName("RequestReturnHandler")
    class Request {

        RequestReturnHandler handler() {
            return new RequestReturnHandler(orderRepository, returnRequestRepository, mapper, eventPublisher);
        }

        RequestReturnCommand cmd() {
            return new RequestReturnCommand(ORDER_ID, USER, "hỏng hàng khi nhận", "REFUND", "[]");
        }

        @Test
        @DisplayName("order not found → OrderNotFoundException")
        void notFound() {
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler().handle(cmd()))
                    .isInstanceOf(OrderNotFoundException.class);
            verifyNoInteractions(returnRequestRepository);
        }

        @Test
        @DisplayName("order in non-returnable state → InvalidOrderStateException")
        void notReturnable() {
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(pendingOrder()));

            assertThatThrownBy(() -> handler().handle(cmd()))
                    .isInstanceOf(InvalidOrderStateException.class);
            verifyNoInteractions(returnRequestRepository);
        }

        @Test
        @DisplayName("delivered > 7 days ago → InvalidOrderStateException (window expired)")
        void windowExpired() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(deliveredOrder(Instant.now().minus(8, ChronoUnit.DAYS))));

            assertThatThrownBy(() -> handler().handle(cmd()))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("7 ngày");
            verifyNoInteractions(returnRequestRepository);
        }

        @Test
        @DisplayName("delivered within window → creates request, publishes requested event, maps DTO")
        void withinWindow_success() {
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(deliveredOrder(Instant.now().minus(2, ChronoUnit.DAYS))));
            var saved = pendingReturn();
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(saved);
            when(mapper.toReturnDto(saved)).thenReturn(dto);

            var result = handler().handle(cmd());

            assertThat(result).isSameAs(dto);
            verify(eventPublisher).publishEventsOf(saved);
            assertThat(saved.peekDomainEvents())
                    .anyMatch(OrderReturnRequestedEvent.class::isInstance);
        }
    }

    // ── ApproveReturnHandler ─────────────────────────────────────────────────

    @Nested
    @DisplayName("ApproveReturnHandler")
    class Approve {

        ApproveReturnHandler handler() {
            return new ApproveReturnHandler(returnRequestRepository, mapper, eventPublisher);
        }

        @Test
        @DisplayName("not found → ReturnRequestNotFoundException")
        void notFound() {
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler().handle(new ApproveReturnCommand(RR_ID, "ok", null)))
                    .isInstanceOf(ReturnRequestNotFoundException.class);
        }

        @Test
        @DisplayName("PENDING → APPROVED, persisted, events published")
        void approvesPending() {
            var rr = pendingReturn();
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.of(rr));
            when(returnRequestRepository.save(rr)).thenReturn(rr);
            when(mapper.toReturnDto(rr)).thenReturn(dto);

            var result = handler().handle(new ApproveReturnCommand(RR_ID, "ok", null));

            assertThat(result).isSameAs(dto);
            assertThat(rr.getStatus()).isEqualTo(ReturnStatus.APPROVED);
            assertThat(rr.getAdminNote()).isEqualTo("ok");
            verify(eventPublisher).publishEventsOf(rr);
        }

        @Test
        @DisplayName("already APPROVED → IllegalStateException, no save")
        void rejectsNonPending() {
            var rr = approvedReturn();
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.of(rr));

            assertThatThrownBy(() -> handler().handle(new ApproveReturnCommand(RR_ID, "ok", null)))
                    .isInstanceOf(IllegalStateException.class);
            verify(returnRequestRepository, never()).save(any());
        }
    }

    // ── RejectReturnHandler ──────────────────────────────────────────────────

    @Nested
    @DisplayName("RejectReturnHandler")
    class Reject {

        RejectReturnHandler handler() {
            return new RejectReturnHandler(returnRequestRepository, mapper, eventPublisher);
        }

        @Test
        @DisplayName("not found → ReturnRequestNotFoundException")
        void notFound() {
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler().handle(new RejectReturnCommand(RR_ID, "không hợp lệ")))
                    .isInstanceOf(ReturnRequestNotFoundException.class);
        }

        @Test
        @DisplayName("PENDING → REJECTED with admin note")
        void rejectsPending() {
            var rr = pendingReturn();
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.of(rr));
            when(returnRequestRepository.save(rr)).thenReturn(rr);
            when(mapper.toReturnDto(rr)).thenReturn(dto);

            handler().handle(new RejectReturnCommand(RR_ID, "không hợp lệ"));

            assertThat(rr.getStatus()).isEqualTo(ReturnStatus.REJECTED);
            assertThat(rr.getAdminNote()).isEqualTo("không hợp lệ");
            verify(eventPublisher).publishEventsOf(rr);
        }
    }

    // ── CompleteReturnHandler ────────────────────────────────────────────────

    @Nested
    @DisplayName("CompleteReturnHandler")
    class Complete {

        CompleteReturnHandler handler() {
            return new CompleteReturnHandler(returnRequestRepository, mapper, eventPublisher);
        }

        @Test
        @DisplayName("not found → ReturnRequestNotFoundException")
        void notFound() {
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler().handle(new CompleteReturnCommand(RR_ID, BigDecimal.TEN)))
                    .isInstanceOf(ReturnRequestNotFoundException.class);
        }

        @Test
        @DisplayName("APPROVED → COMPLETED with refund amount")
        void completesApproved() {
            var rr = approvedReturn();
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.of(rr));
            when(returnRequestRepository.save(rr)).thenReturn(rr);
            when(mapper.toReturnDto(rr)).thenReturn(dto);

            handler().handle(new CompleteReturnCommand(RR_ID, BigDecimal.valueOf(99_000)));

            assertThat(rr.getStatus()).isEqualTo(ReturnStatus.COMPLETED);
            assertThat(rr.getRefundAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(99_000));
            verify(eventPublisher).publishEventsOf(rr);
        }

        @Test
        @DisplayName("PENDING (not APPROVED) → IllegalStateException, no save")
        void rejectsNonApproved() {
            var rr = pendingReturn();
            when(returnRequestRepository.findById(ReturnRequestId.of(RR_ID))).thenReturn(Optional.of(rr));

            assertThatThrownBy(() -> handler().handle(new CompleteReturnCommand(RR_ID, BigDecimal.TEN)))
                    .isInstanceOf(IllegalStateException.class);
            verify(returnRequestRepository, never()).save(any());
        }
    }
}
