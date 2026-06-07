package com.hieu.cart_service.grpc.server;

import com.hieu.cart_service.dto.CartDTO;
import com.hieu.cart_service.dto.CartItemDTO;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartResponse;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartResponse;
import com.hieu.cart_service.service.CartService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link CartGrpcService}: the CartService is mocked and only the
 * DTO -> protobuf mapping (incl. null coalescing) and the success/error StreamObserver
 * protocol are asserted. No gRPC server, no Spring, no containers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartGrpcService (unit)")
class CartGrpcServiceTest {

    @Mock CartService cartService;
    @Mock StreamObserver<GetCartResponse> getObserver;
    @Mock StreamObserver<ClearCartResponse> clearObserver;

    private CartGrpcService service() {
        return new CartGrpcService(cartService);
    }

    private static CartItemDTO fullItem() {
        return new CartItemDTO(
                "7", "10", "Widget", "100", "SKU-100", "img.png",
                new BigDecimal("12.50"), 3, new BigDecimal("37.50"), null, null);
    }

    @Test
    @DisplayName("getCart maps a populated CartDTO into a GetCartResponse with one snapshot")
    void getCart_mapsItems() {
        var cart = new CartDTO("u1", List.of(fullItem()), 1, new BigDecimal("37.50"), List.of());
        when(cartService.getCart("u1")).thenReturn(cart);

        service().getCart(GetCartRequest.newBuilder().setUserId("u1").build(), getObserver);

        var captor = ArgumentCaptor.forClass(GetCartResponse.class);
        verify(getObserver).onNext(captor.capture());
        verify(getObserver).onCompleted();
        verify(getObserver, never()).onError(any());

        GetCartResponse resp = captor.getValue();
        assertThat(resp.getUserId()).isEqualTo("u1");
        assertThat(resp.getItemsList()).hasSize(1);
        var snap = resp.getItemsList().get(0);
        assertThat(snap.getId()).isEqualTo("7");
        assertThat(snap.getUserId()).isEqualTo("u1");
        assertThat(snap.getProductId()).isEqualTo("10");
        assertThat(snap.getProductName()).isEqualTo("Widget");
        assertThat(snap.getVariantId()).isEqualTo("100");
        assertThat(snap.getVariantSku()).isEqualTo("SKU-100");
        assertThat(snap.getVariantImage()).isEqualTo("img.png");
        assertThat(snap.getUnitPrice()).isEqualTo("12.50");
        assertThat(snap.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("getCart coalesces null id/price/name fields into proto zero/empty defaults")
    void getCart_nullFields_coalesced() {
        var sparse = new CartItemDTO(
                null, null, null, null, null, null, null, null, null, null, null);
        var cart = new CartDTO("u1", List.of(sparse), 1, BigDecimal.ZERO, List.of());
        when(cartService.getCart("u1")).thenReturn(cart);

        service().getCart(GetCartRequest.newBuilder().setUserId("u1").build(), getObserver);

        var captor = ArgumentCaptor.forClass(GetCartResponse.class);
        verify(getObserver).onNext(captor.capture());
        var snap = captor.getValue().getItemsList().get(0);
        assertThat(snap.getId()).isEmpty();
        assertThat(snap.getProductId()).isEmpty();
        assertThat(snap.getProductName()).isEmpty();
        assertThat(snap.getVariantId()).isEmpty();
        assertThat(snap.getVariantSku()).isEmpty();
        assertThat(snap.getVariantImage()).isEmpty();
        assertThat(snap.getUnitPrice()).isEqualTo("0");
        assertThat(snap.getQuantity()).isZero();
    }

    @Test
    @DisplayName("getCart for an empty cart emits an empty items list and completes")
    void getCart_emptyCart() {
        var cart = new CartDTO("u1", List.of(), 0, BigDecimal.ZERO, List.of());
        when(cartService.getCart("u1")).thenReturn(cart);

        service().getCart(GetCartRequest.newBuilder().setUserId("u1").build(), getObserver);

        var captor = ArgumentCaptor.forClass(GetCartResponse.class);
        verify(getObserver).onNext(captor.capture());
        verify(getObserver).onCompleted();
        assertThat(captor.getValue().getItemsList()).isEmpty();
    }

    @Test
    @DisplayName("getCart propagates a service failure as gRPC INTERNAL and never completes")
    void getCart_serviceFailure_propagatesInternal() {
        when(cartService.getCart("u1")).thenThrow(new RuntimeException("redis down"));

        service().getCart(GetCartRequest.newBuilder().setUserId("u1").build(), getObserver);

        var captor = ArgumentCaptor.forClass(Throwable.class);
        verify(getObserver).onError(captor.capture());
        verify(getObserver, never()).onNext(any());
        verify(getObserver, never()).onCompleted();

        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        var sre = (StatusRuntimeException) captor.getValue();
        assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(sre.getStatus().getDescription()).contains("redis down");
    }

    @Test
    @DisplayName("clearCart delegates to the service and replies success=true")
    void clearCart_success() {
        service().clearCart(ClearCartRequest.newBuilder().setUserId("u1").build(), clearObserver);

        verify(cartService).clearCart("u1");
        var captor = ArgumentCaptor.forClass(ClearCartResponse.class);
        verify(clearObserver).onNext(captor.capture());
        verify(clearObserver).onCompleted();
        verify(clearObserver, never()).onError(any());
        assertThat(captor.getValue().getSuccess()).isTrue();
    }

    @Test
    @DisplayName("clearCart propagates a service failure as gRPC INTERNAL and never completes")
    void clearCart_serviceFailure_propagatesInternal() {
        org.mockito.Mockito.doThrow(new RuntimeException("db error"))
                .when(cartService).clearCart("u1");

        service().clearCart(ClearCartRequest.newBuilder().setUserId("u1").build(), clearObserver);

        var captor = ArgumentCaptor.forClass(Throwable.class);
        verify(clearObserver).onError(captor.capture());
        verify(clearObserver, never()).onNext(any());
        verify(clearObserver, never()).onCompleted();

        var sre = (StatusRuntimeException) captor.getValue();
        assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(sre.getStatus().getDescription()).contains("db error");
    }
}
