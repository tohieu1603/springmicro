package com.hieu.order_service.interfaces.grpc;

import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.dto.OrderItemDTO;
import com.hieu.order_service.application.handler.order.GetOrderByIdInternalHandler;
import com.hieu.order_service.application.handler.order.GetOrderByNumberHandler;
import com.hieu.order_service.application.query.order.GetOrderByIdInternalQuery;
import com.hieu.order_service.application.query.order.GetOrderByNumberQuery;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.interfaces.grpc.proto.GetOrderByNumberRequest;
import com.hieu.order_service.interfaces.grpc.proto.GetOrderRequest;
import com.hieu.order_service.interfaces.grpc.proto.GetOrderResponse;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests for the gRPC <-> domain DTO/protobuf mapping. Handlers are mocked. */
@ExtendWith(MockitoExtension.class)
class OrderGrpcServiceTest {

    @Mock GetOrderByIdInternalHandler getByIdHandler;
    @Mock GetOrderByNumberHandler getByNumberHandler;
    @Mock StreamObserver<GetOrderResponse> observer;

    @InjectMocks OrderGrpcService service;

    private OrderDTO fullDto() {
        return new OrderDTO(
                "00000000-0000-0000-0000-000000000010", "ORD-20240101-000001", "user-1", "CONFIRMED",
                List.of(new OrderItemDTO("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000900", "Widget", "00000000-0000-0000-0000-000000000901", "SKU-1", "img.png",
                        new BigDecimal("50.00"), 2, new BigDecimal("100.00"))),
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("5.00"), new BigDecimal("105.00"),
                "VC", "Recipient", "0901234567",
                "12 Le Loi", "Ward", "District", "City", "Vietnam", "70000",
                "notes", "COD", "00000000-0000-0000-0000-000000000042", "res", "00000000-0000-0000-0000-000000000007", null,
                Instant.now(), Instant.now(), null, null, null, null, null);
    }

    @Test
    @DisplayName("getOrder maps the DTO into a found GetOrderResponse including items")
    void getOrder_mapsDto() {
        when(getByIdHandler.handle(any(GetOrderByIdInternalQuery.class))).thenReturn(fullDto());

        service.getOrder(GetOrderRequest.newBuilder().setOrderId("00000000-0000-0000-0000-000000000010").build(), observer);

        var captor = ArgumentCaptor.forClass(GetOrderResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        var resp = captor.getValue();
        assertThat(resp.getFound()).isTrue();
        assertThat(resp.getId()).isEqualTo("00000000-0000-0000-0000-000000000010");
        assertThat(resp.getOrderNumber()).isEqualTo("ORD-20240101-000001");
        assertThat(resp.getUserId()).isEqualTo("user-1");
        assertThat(resp.getStatus()).isEqualTo("CONFIRMED");
        assertThat(resp.getTotalAmount()).isEqualTo("105.00");
        assertThat(resp.getRecipientName()).isEqualTo("Recipient");
        assertThat(resp.getRecipientPhone()).isEqualTo("0901234567");
        assertThat(resp.getAddressLine()).isEqualTo("12 Le Loi");
        assertThat(resp.getWard()).isEqualTo("Ward");
        assertThat(resp.getDistrict()).isEqualTo("District");
        assertThat(resp.getCity()).isEqualTo("City");
        assertThat(resp.getItemsCount()).isEqualTo(1);
        var item = resp.getItems(0);
        assertThat(item.getVariantId()).isEqualTo("00000000-0000-0000-0000-000000000901");
        assertThat(item.getSku()).isEqualTo("SKU-1");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualTo("50.00");
    }

    @Test
    @DisplayName("getOrder returns found=false when the order is missing")
    void getOrder_notFound() {
        when(getByIdHandler.handle(any(GetOrderByIdInternalQuery.class)))
                .thenThrow(new OrderNotFoundException(99L));

        service.getOrder(GetOrderRequest.newBuilder().setOrderId("00000000-0000-0000-0000-000000000099").build(), observer);

        var captor = ArgumentCaptor.forClass(GetOrderResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getFound()).isFalse();
    }

    @Test
    @DisplayName("getOrder propagates an unexpected error as INTERNAL and does not complete")
    void getOrder_unexpectedError() {
        when(getByIdHandler.handle(any(GetOrderByIdInternalQuery.class)))
                .thenThrow(new RuntimeException("boom"));

        service.getOrder(GetOrderRequest.newBuilder().setOrderId("00000000-0000-0000-0000-000000000001").build(), observer);

        verify(observer).onError(any(StatusRuntimeException.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }

    @Test
    @DisplayName("getOrderByNumber maps the DTO and queries by number with admin bypass")
    void getOrderByNumber_mapsDto() {
        when(getByNumberHandler.handle(any(GetOrderByNumberQuery.class))).thenReturn(fullDto());

        service.getOrderByNumber(
                GetOrderByNumberRequest.newBuilder().setOrderNumber("ORD-20240101-000001").build(), observer);

        var queryCaptor = ArgumentCaptor.forClass(GetOrderByNumberQuery.class);
        verify(getByNumberHandler).handle(queryCaptor.capture());
        assertThat(queryCaptor.getValue().orderNumber()).isEqualTo("ORD-20240101-000001");
        assertThat(queryCaptor.getValue().isAdmin()).isTrue();

        var captor = ArgumentCaptor.forClass(GetOrderResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getFound()).isTrue();
    }

    @Test
    @DisplayName("getOrderByNumber returns found=false when the order is missing")
    void getOrderByNumber_notFound() {
        when(getByNumberHandler.handle(any(GetOrderByNumberQuery.class)))
                .thenThrow(new OrderNotFoundException("X"));

        service.getOrderByNumber(
                GetOrderByNumberRequest.newBuilder().setOrderNumber("X").build(), observer);

        var captor = ArgumentCaptor.forClass(GetOrderResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getFound()).isFalse();
    }

    @Test
    @DisplayName("buildResponse substitutes defaults for null item variantId/sku")
    void getOrder_nullItemFields() {
        var dto = new OrderDTO(
                "00000000-0000-0000-0000-000000000010", "ORD-20240101-000001", "user-1", "PENDING",
                List.of(new OrderItemDTO("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000900", "Widget", null, null, null,
                        new BigDecimal("50.00"), 3, new BigDecimal("150.00"))),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("150.00"),
                null, "R", "0901234567", "s", "w", "d", "c", "Vietnam", null,
                null, "COD", null, null, null, null,
                Instant.now(), Instant.now(), null, null, null, null, null);
        when(getByIdHandler.handle(any(GetOrderByIdInternalQuery.class))).thenReturn(dto);

        service.getOrder(GetOrderRequest.newBuilder().setOrderId("00000000-0000-0000-0000-000000000010").build(), observer);

        var captor = ArgumentCaptor.forClass(GetOrderResponse.class);
        verify(observer).onNext(captor.capture());
        var item = captor.getValue().getItems(0);
        assertThat(item.getVariantId()).isEmpty();
        assertThat(item.getSku()).isEmpty();
        assertThat(item.getQuantity()).isEqualTo(3);
    }
}
