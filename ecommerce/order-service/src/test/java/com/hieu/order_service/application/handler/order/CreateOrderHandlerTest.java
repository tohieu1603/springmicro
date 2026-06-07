package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.command.order.CreateOrderCommand;
import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.common.ValidationException;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.saga.OrderSagaOrchestrator;
import com.hieu.order_service.application.service.IdempotencyService;
import com.hieu.order_service.application.service.OrderNumberService;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.domain.service.OrderDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link CreateOrderHandler} orchestration: payload
 * validation, idempotency short-circuit, the persist → saga → markCompleted
 * happy path, and the no-idempotency-key branch. The domain service builds a
 * real {@link Order}; the saga and idempotency service are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderHandler")
class CreateOrderHandlerTest {

    private static final String USER    = "11111111-1111-1111-1111-111111111111";
    private static final String PROD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String VAR_ID  = "33333333-3333-3333-3333-333333333333";

    @Mock OrderRepository orderRepository;
    @Mock OrderDomainService domainService;
    @Mock OrderDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;
    @Mock OrderSagaOrchestrator saga;
    @Mock IdempotencyService idempotencyService;
    @Mock OrderNumberService orderNumberService;

    @InjectMocks CreateOrderHandler handler;

    private final OrderDTO sagaDto = mock(OrderDTO.class);

    static CreateOrderCommand validCmd(String idempotencyKey) {
        return new CreateOrderCommand(
                USER,
                List.of(new CreateOrderCommand.ItemCmd(
                        PROD_ID, "Product A", VAR_ID, "SKU-001", null,
                        BigDecimal.valueOf(100_000), 2)),
                "Nguyen Van A", "0901234567",
                "123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000",
                "COD", "note", null, idempotencyKey, "jwt-token");
    }

    /** A domain Order with id assigned, as domainService.createOrder + save would yield. */
    static Order savedOrder() {
        return OrderTestSupport.order(USER);
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("blank userId → ValidationException with field error")
        void blankUserId() {
            var cmd = new CreateOrderCommand(
                    "  ", List.of(new CreateOrderCommand.ItemCmd(PROD_ID, "P", VAR_ID, "S", null, BigDecimal.ONE, 1)),
                    "name", "phone", "s", "w", "d", "c", "VN", "70000", "COD", null, null, null, null);

            assertThatThrownBy(() -> handler.handle(cmd))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).fieldErrors()).containsKey("userId"));
            verifyNoInteractions(saga);
        }

        @Test
        @DisplayName("empty items → ValidationException")
        void emptyItems() {
            var cmd = new CreateOrderCommand(
                    USER, List.of(), "name", "phone", "s", "w", "d", "c", "VN", "70000",
                    "COD", null, null, null, null);

            assertThatThrownBy(() -> handler.handle(cmd))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).fieldErrors()).containsKey("items"));
        }

        @Test
        @DisplayName("missing recipient/phone/paymentMethod → all reported")
        void multipleMissing() {
            var cmd = new CreateOrderCommand(
                    USER, List.of(new CreateOrderCommand.ItemCmd(PROD_ID, "P", VAR_ID, "S", null, BigDecimal.ONE, 1)),
                    null, null, "s", "w", "d", "c", "VN", "70000", null, null, null, null, null);

            assertThatThrownBy(() -> handler.handle(cmd))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> {
                        var fe = ((ValidationException) e).fieldErrors();
                        assertThat(fe).containsKeys("recipientName", "recipientPhone", "paymentMethod");
                    });
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("existing idempotency record → cached DTO returned, saga skipped")
        void cachedHit_shortCircuits() {
            var cmd = validCmd("idem-key");
            when(idempotencyService.checkOrCreate(USER, "idem-key")).thenReturn(Optional.of(sagaDto));

            var result = handler.handle(cmd);

            assertThat(result).isSameAs(sagaDto);
            verifyNoInteractions(saga);
            verifyNoInteractions(domainService);
            verify(idempotencyService, never()).markCompleted(any(), anyString(), any());
        }

        @Test
        @DisplayName("idempotency miss → builds, runs saga, marks completed")
        void miss_runsFullFlow() {
            var cmd = validCmd("idem-key");
            var order = savedOrder();
            when(idempotencyService.checkOrCreate(USER, "idem-key")).thenReturn(Optional.empty());
            when(domainService.createOrder(any(), any(), any(), any(), any(), eq("COD"), eq("note"), any(), eq("idem-key"), any(), eq(USER)))
                    .thenReturn(order);
            when(orderRepository.save(order)).thenReturn(order);
            when(orderNumberService.next()).thenReturn("ORD-20260101-000001");
            when(saga.executeCreateOrderSaga(order.getId().value(), "jwt-token")).thenReturn(sagaDto);

            var result = handler.handle(cmd);

            assertThat(result).isSameAs(sagaDto);
            verify(eventPublisher).publishEventsOf(order);
            verify(saga).executeCreateOrderSaga(order.getId().value(), "jwt-token");
            verify(idempotencyService).markCompleted("idem-key", order.getId().value(), sagaDto);
        }
    }

    @Nested
    @DisplayName("no idempotency key")
    class NoIdempotencyKey {

        @Test
        @DisplayName("null key → idempotency service untouched, saga runs, no markCompleted")
        void nullKey_skipsIdempotency() {
            var cmd = validCmd(null);
            var order = savedOrder();
            when(domainService.createOrder(any(), any(), any(), any(), any(), eq("COD"), eq("note"), any(), eq(null), any(), eq(USER)))
                    .thenReturn(order);
            when(orderRepository.save(order)).thenReturn(order);
            when(orderNumberService.next()).thenReturn("ORD-20260101-000001");
            when(saga.executeCreateOrderSaga(order.getId().value(), "jwt-token")).thenReturn(sagaDto);

            var result = handler.handle(cmd);

            assertThat(result).isSameAs(sagaDto);
            verifyNoInteractions(idempotencyService);
            verify(eventPublisher).publishEventsOf(order);
        }
    }
}
