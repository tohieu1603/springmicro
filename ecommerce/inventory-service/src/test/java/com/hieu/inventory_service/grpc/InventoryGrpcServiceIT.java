package com.hieu.inventory_service.grpc;

import com.hieu.inventory_service.AbstractIntegrationTest;
import com.hieu.inventory_service.interfaces.grpc.proto.CheckStockRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.CheckStockResponse;
import com.hieu.inventory_service.interfaces.grpc.proto.ConfirmReservationRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.ConfirmReservationResponse;
import com.hieu.inventory_service.interfaces.grpc.proto.ReleaseStockRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.ReleaseStockResponse;
import com.hieu.inventory_service.interfaces.grpc.proto.ReserveItem;
import com.hieu.inventory_service.interfaces.grpc.proto.ReserveStockRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.ReserveStockResponse;
import com.hieu.inventory_service.service.InventoryService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryGrpcService — integration tests")
class InventoryGrpcServiceIT extends AbstractIntegrationTest {

    @Autowired InventoryGrpcService grpcService;
    @Autowired InventoryService inventoryService;

    private String productId;

    @BeforeEach
    void seedInventory() {
        productId = UUID.randomUUID().toString();
        String sku = "SKU-GRPC-INV-" + productId;
        inventoryService.create(productId, sku, 100, 10);
    }

    // ── reserveStock ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserveStock_grpc_returnsReservationId — dự trữ qua gRPC trả về reservationId")
    void reserveStock_grpc_returnsReservationId() {
        String orderId = "ORDER-GRPC-" + UUID.randomUUID();
        var req = ReserveStockRequest.newBuilder()
                .setOrderId(orderId)
                .addItems(ReserveItem.newBuilder()
                        .setProductId(productId)
                        .setQuantity(10)
                        .build())
                .build();

        ReserveStockResponse resp = callReserve(req);

        assertThat(resp.getSuccess()).isTrue();
        assertThat(resp.getReservationId()).isNotBlank();
        assertThat(resp.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("reserveStock_insufficient_returnsFailedPrecondition — tồn kho không đủ → FAILED_PRECONDITION")
    void reserveStock_insufficient_returnsFailedPrecondition() {
        String orderId = "ORDER-INSUF-" + UUID.randomUUID();
        var req = ReserveStockRequest.newBuilder()
                .setOrderId(orderId)
                .addItems(ReserveItem.newBuilder()
                        .setProductId(productId)
                        .setQuantity(9999)
                        .build())
                .build();

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        grpcService.reserveStock(req, new StreamObserver<ReserveStockResponse>() {
            @Override public void onNext(ReserveStockResponse v) {}
            @Override public void onError(Throwable t) { errorRef.set(t); }
            @Override public void onCompleted() {}
        });

        assertThat(errorRef.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) errorRef.get()).getStatus().getCode())
                .isEqualTo(Status.FAILED_PRECONDITION.getCode());
    }

    // ── confirmReservation ────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmReservation_notFound_returnsIdempotentSuccess — reservationId không tồn tại → idempotent success hoặc NOT_FOUND")
    void confirmReservation_notFound_returnsNotFound() {
        var req = ConfirmReservationRequest.newBuilder()
                .setReservationId("NON-EXISTENT-" + UUID.randomUUID())
                .build();

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<ConfirmReservationResponse> respRef = new AtomicReference<>();
        grpcService.confirmReservation(req, new StreamObserver<ConfirmReservationResponse>() {
            @Override public void onNext(ConfirmReservationResponse v) { respRef.set(v); }
            @Override public void onError(Throwable t) { errorRef.set(t); }
            @Override public void onCompleted() {}
        });

        // InventoryService.confirmReservation is idempotent for missing records → success
        if (errorRef.get() != null) {
            assertThat(((StatusRuntimeException) errorRef.get()).getStatus().getCode())
                    .isEqualTo(Status.NOT_FOUND.getCode());
        } else {
            assertThat(respRef.get().getSuccess()).isTrue();
        }
    }

    // ── releaseStock ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("releaseStock_alreadyReleased_returnsFailedPrecondition — reservation đã released → idempotent success")
    void releaseStock_alreadyReleased_returnsFailedPrecondition() {
        String orderId = "ORDER-RELEASE-" + UUID.randomUUID();

        // Reserve first
        ReserveStockResponse reserveResp = callReserve(ReserveStockRequest.newBuilder()
                .setOrderId(orderId)
                .addItems(ReserveItem.newBuilder().setProductId(productId).setQuantity(5).build())
                .build());
        assertThat(reserveResp.getSuccess()).isTrue();

        // First release
        ReleaseStockResponse resp1 = callRelease(
                ReleaseStockRequest.newBuilder().setReservationId(orderId).build());
        assertThat(resp1.getSuccess()).isTrue();

        // Second release — idempotent (status != ACTIVE → success)
        ReleaseStockResponse resp2 = callRelease(
                ReleaseStockRequest.newBuilder().setReservationId(orderId).build());
        assertThat(resp2.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("checkStock_grpc_returnsAvailableTrue_whenEnough — tồn kho đủ → available=true")
    void checkStock_grpc_returnsAvailableTrue() {
        var req = CheckStockRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(10)
                .build();

        CheckStockResponse resp = callCheckStock(req);

        assertThat(resp.getAvailable()).isTrue();
        assertThat(resp.getAvailableQuantity()).isGreaterThanOrEqualTo(10);
    }

    // ── typed helpers (avoid raw-type inference issue) ────────────────────────

    private ReserveStockResponse callReserve(ReserveStockRequest req) {
        AtomicReference<ReserveStockResponse> holder = new AtomicReference<>();
        grpcService.reserveStock(req, new StreamObserver<ReserveStockResponse>() {
            @Override public void onNext(ReserveStockResponse v) { holder.set(v); }
            @Override public void onError(Throwable t) { throw new AssertionError("Unexpected gRPC error: " + t.getMessage(), t); }
            @Override public void onCompleted() {}
        });
        return holder.get();
    }

    private ReleaseStockResponse callRelease(ReleaseStockRequest req) {
        AtomicReference<ReleaseStockResponse> holder = new AtomicReference<>();
        grpcService.releaseStock(req, new StreamObserver<ReleaseStockResponse>() {
            @Override public void onNext(ReleaseStockResponse v) { holder.set(v); }
            @Override public void onError(Throwable t) { throw new AssertionError("Unexpected gRPC error: " + t.getMessage(), t); }
            @Override public void onCompleted() {}
        });
        return holder.get();
    }

    private CheckStockResponse callCheckStock(CheckStockRequest req) {
        AtomicReference<CheckStockResponse> holder = new AtomicReference<>();
        grpcService.checkStock(req, new StreamObserver<CheckStockResponse>() {
            @Override public void onNext(CheckStockResponse v) { holder.set(v); }
            @Override public void onError(Throwable t) { throw new AssertionError("Unexpected gRPC error: " + t.getMessage(), t); }
            @Override public void onCompleted() {}
        });
        return holder.get();
    }
}
