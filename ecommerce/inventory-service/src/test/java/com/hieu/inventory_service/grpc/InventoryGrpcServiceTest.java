package com.hieu.inventory_service.grpc;

import com.hieu.inventory_service.dto.InventoryDTO;
import com.hieu.inventory_service.dto.ReservationRequest;
import com.hieu.inventory_service.dto.ReservationResult;
import com.hieu.inventory_service.exception.InsufficientStockException;
import com.hieu.inventory_service.exception.InventoryNotFoundException;
import com.hieu.inventory_service.interfaces.grpc.proto.*;
import com.hieu.inventory_service.service.InventoryService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link InventoryGrpcService}: protobuf-request -> domain-DTO ->
 * protobuf-response mapping and the domain-exception -> gRPC-{@link Status} translation.
 * The {@link InventoryService} is mocked; a capturing {@link StreamObserver} records the
 * emitted value / error. (The IT exercises the same paths against a live service + Redis;
 * here we drive the error branches deterministically with no infrastructure.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryGrpcService (unit)")
class InventoryGrpcServiceTest {

    @Mock InventoryService inventoryService;
    @Captor ArgumentCaptor<ReservationRequest> reservationCaptor;

    InventoryGrpcService grpcService;

    @BeforeEach
    void setup() {
        grpcService = new InventoryGrpcService(inventoryService);
    }

    /** Records exactly one of onNext / onError so assertions can branch on the outcome. */
    private static final class Capture<T> implements StreamObserver<T> {
        T value;
        Throwable error;
        boolean completed;
        @Override public void onNext(T v) { this.value = v; }
        @Override public void onError(Throwable t) { this.error = t; }
        @Override public void onCompleted() { this.completed = true; }
    }

    private static Status.Code codeOf(Throwable t) {
        assertThat(t).isInstanceOf(StatusRuntimeException.class);
        return ((StatusRuntimeException) t).getStatus().getCode();
    }

    // ── checkStock ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkStock maps available=true when stock >= requested")
    void checkStock_available() {
        when(inventoryService.getByProductId("5"))
                .thenReturn(InventoryDTO.builder().productId("5").availableQuantity(30).build());
        var obs = new Capture<CheckStockResponse>();

        grpcService.checkStock(CheckStockRequest.newBuilder().setProductId("5").setQuantity(10).build(), obs);

        assertThat(obs.error).isNull();
        assertThat(obs.completed).isTrue();
        assertThat(obs.value.getAvailable()).isTrue();
        assertThat(obs.value.getAvailableQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("checkStock maps available=false when stock < requested")
    void checkStock_insufficient() {
        when(inventoryService.getByProductId("5"))
                .thenReturn(InventoryDTO.builder().productId("5").availableQuantity(3).build());
        var obs = new Capture<CheckStockResponse>();

        grpcService.checkStock(CheckStockRequest.newBuilder().setProductId("5").setQuantity(10).build(), obs);

        assertThat(obs.value.getAvailable()).isFalse();
        assertThat(obs.value.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("checkStock translates InventoryNotFoundException -> NOT_FOUND")
    void checkStock_notFound() {
        when(inventoryService.getByProductId("9")).thenThrow(new InventoryNotFoundException("9"));
        var obs = new Capture<CheckStockResponse>();

        grpcService.checkStock(CheckStockRequest.newBuilder().setProductId("9").setQuantity(1).build(), obs);

        assertThat(obs.value).isNull();
        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("checkStock translates unexpected error -> INTERNAL")
    void checkStock_internal() {
        when(inventoryService.getByProductId("9")).thenThrow(new RuntimeException("db down"));
        var obs = new Capture<CheckStockResponse>();

        grpcService.checkStock(CheckStockRequest.newBuilder().setProductId("9").setQuantity(1).build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.INTERNAL);
    }

    // ── reserveStock ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserveStock maps proto items -> ReservationRequest and copies the result")
    void reserveStock_success() {
        when(inventoryService.reserveStock(any(ReservationRequest.class)))
                .thenReturn(ReservationResult.success("ORDER-1"));
        var obs = new Capture<ReserveStockResponse>();

        var req = ReserveStockRequest.newBuilder()
                .setOrderId("ORDER-1")
                .addItems(ReserveItem.newBuilder().setProductId("100").setQuantity(4).build())
                .addItems(ReserveItem.newBuilder().setProductId("200").setQuantity(7).build())
                .build();
        grpcService.reserveStock(req, obs);

        verify(inventoryService).reserveStock(reservationCaptor.capture());
        ReservationRequest mapped = reservationCaptor.getValue();
        assertThat(mapped.orderId()).isEqualTo("ORDER-1");
        assertThat(mapped.items()).hasSize(2);
        assertThat(mapped.items().get(0).productId()).isEqualTo("100");
        assertThat(mapped.items().get(0).quantity()).isEqualTo(4);
        assertThat(mapped.items().get(1).productId()).isEqualTo("200");

        assertThat(obs.error).isNull();
        assertThat(obs.completed).isTrue();
        assertThat(obs.value.getSuccess()).isTrue();
        assertThat(obs.value.getReservationId()).isEqualTo("ORDER-1");
        assertThat(obs.value.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("reserveStock null reservationId / errorMessage are coerced to empty strings")
    void reserveStock_nullFieldsCoercedToEmpty() {
        // failure() => reservationId == null; manually craft a result with null error too
        when(inventoryService.reserveStock(any(ReservationRequest.class)))
                .thenReturn(new ReservationResult(false, null, null));
        var obs = new Capture<ReserveStockResponse>();

        grpcService.reserveStock(ReserveStockRequest.newBuilder().setOrderId("O").build(), obs);

        assertThat(obs.value.getSuccess()).isFalse();
        assertThat(obs.value.getReservationId()).isEmpty();
        assertThat(obs.value.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("reserveStock translates InsufficientStockException -> FAILED_PRECONDITION")
    void reserveStock_insufficient() {
        when(inventoryService.reserveStock(any(ReservationRequest.class)))
                .thenThrow(new InsufficientStockException("1", 0, 5));
        var obs = new Capture<ReserveStockResponse>();

        grpcService.reserveStock(ReserveStockRequest.newBuilder().setOrderId("O").build(), obs);

        assertThat(obs.value).isNull();
        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.FAILED_PRECONDITION);
    }

    @Test
    @DisplayName("reserveStock translates InventoryNotFoundException -> NOT_FOUND")
    void reserveStock_notFound() {
        when(inventoryService.reserveStock(any(ReservationRequest.class)))
                .thenThrow(new InventoryNotFoundException("1"));
        var obs = new Capture<ReserveStockResponse>();

        grpcService.reserveStock(ReserveStockRequest.newBuilder().setOrderId("O").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("reserveStock translates unexpected error -> INTERNAL")
    void reserveStock_internal() {
        when(inventoryService.reserveStock(any(ReservationRequest.class)))
                .thenThrow(new RuntimeException("kaboom"));
        var obs = new Capture<ReserveStockResponse>();

        grpcService.reserveStock(ReserveStockRequest.newBuilder().setOrderId("O").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.INTERNAL);
    }

    // ── confirmReservation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmReservation forwards the reservationId and copies success")
    void confirmReservation_success() {
        when(inventoryService.confirmReservation("R-1")).thenReturn(ReservationResult.success("R-1"));
        var obs = new Capture<ConfirmReservationResponse>();

        grpcService.confirmReservation(
                ConfirmReservationRequest.newBuilder().setReservationId("R-1").build(), obs);

        verify(inventoryService).confirmReservation(eq("R-1"));
        assertThat(obs.completed).isTrue();
        assertThat(obs.value.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("confirmReservation translates InventoryNotFoundException -> NOT_FOUND")
    void confirmReservation_notFound() {
        when(inventoryService.confirmReservation("R-1")).thenThrow(new InventoryNotFoundException("R-1"));
        var obs = new Capture<ConfirmReservationResponse>();

        grpcService.confirmReservation(
                ConfirmReservationRequest.newBuilder().setReservationId("R-1").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("confirmReservation translates IllegalStateException -> FAILED_PRECONDITION")
    void confirmReservation_illegalState() {
        when(inventoryService.confirmReservation("R-1"))
                .thenThrow(new IllegalStateException("not active"));
        var obs = new Capture<ConfirmReservationResponse>();

        grpcService.confirmReservation(
                ConfirmReservationRequest.newBuilder().setReservationId("R-1").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.FAILED_PRECONDITION);
    }

    @Test
    @DisplayName("confirmReservation translates unexpected error -> INTERNAL")
    void confirmReservation_internal() {
        when(inventoryService.confirmReservation("R-1")).thenThrow(new RuntimeException("x"));
        var obs = new Capture<ConfirmReservationResponse>();

        grpcService.confirmReservation(
                ConfirmReservationRequest.newBuilder().setReservationId("R-1").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.INTERNAL);
    }

    // ── releaseStock ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("releaseStock forwards the reservationId and copies success")
    void releaseStock_success() {
        when(inventoryService.releaseReservation("R-2")).thenReturn(ReservationResult.success("R-2"));
        var obs = new Capture<ReleaseStockResponse>();

        grpcService.releaseStock(
                ReleaseStockRequest.newBuilder().setReservationId("R-2").build(), obs);

        verify(inventoryService).releaseReservation(eq("R-2"));
        assertThat(obs.completed).isTrue();
        assertThat(obs.value.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("releaseStock translates InventoryNotFoundException -> NOT_FOUND")
    void releaseStock_notFound() {
        when(inventoryService.releaseReservation("R-2")).thenThrow(new InventoryNotFoundException("R-2"));
        var obs = new Capture<ReleaseStockResponse>();

        grpcService.releaseStock(
                ReleaseStockRequest.newBuilder().setReservationId("R-2").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("releaseStock translates IllegalStateException -> FAILED_PRECONDITION")
    void releaseStock_illegalState() {
        when(inventoryService.releaseReservation("R-2"))
                .thenThrow(new IllegalStateException("already released"));
        var obs = new Capture<ReleaseStockResponse>();

        grpcService.releaseStock(
                ReleaseStockRequest.newBuilder().setReservationId("R-2").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.FAILED_PRECONDITION);
    }

    @Test
    @DisplayName("releaseStock translates unexpected error -> INTERNAL")
    void releaseStock_internal() {
        when(inventoryService.releaseReservation("R-2")).thenThrow(new RuntimeException("x"));
        var obs = new Capture<ReleaseStockResponse>();

        grpcService.releaseStock(
                ReleaseStockRequest.newBuilder().setReservationId("R-2").build(), obs);

        assertThat(codeOf(obs.error)).isEqualTo(Status.Code.INTERNAL);
        verify(inventoryService, never()).confirmReservation(any());
    }
}
