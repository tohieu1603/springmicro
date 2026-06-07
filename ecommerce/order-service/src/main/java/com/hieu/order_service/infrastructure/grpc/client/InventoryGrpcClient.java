package com.hieu.order_service.infrastructure.grpc.client;

import com.hieu.inventory_service.interfaces.grpc.proto.CheckStockRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.CheckStockResponse;
import com.hieu.inventory_service.interfaces.grpc.proto.ConfirmReservationRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.InventoryServiceGrpc;
import com.hieu.inventory_service.interfaces.grpc.proto.ReleaseStockRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.ReserveItem;
import com.hieu.inventory_service.interfaces.grpc.proto.ReserveStockRequest;
import com.hieu.inventory_service.interfaces.grpc.proto.ReserveStockResponse;
import com.hieu.order_service.domain.exception.InsufficientStockException;
import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for inventory-service. Matches the real {@code inventory.proto} contract:
 * {@code product_id + quantity} per item, {@code order_id} string as reservation key,
 * and uses {@code ConfirmReservation} for the PAID→committed transition.
 *
 * <p>Failure classification:
 * <ul>
 *   <li>{@code response.success = false} → {@link InsufficientStockException} (business error, no retry).</li>
 *   <li>{@link StatusRuntimeException} → {@link ServiceUnavailableException} so the saga
 *       can run compensation instead of conflating transport failures with stock shortages.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryGrpcClient {

    /** Bounded deadline so a hung inventory-service can't pin the saga thread. Reserve
     *  is interactive (user is waiting), 3s is generous given the typical sub-100ms p99. */
    private static final long DEADLINE_MS = 3000;

    private final InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    private InventoryServiceGrpc.InventoryServiceBlockingStub deadlined() {
        return stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS);
    }

    public String reserveStock(String orderId, List<ReserveItemInput> items) {
        log.debug("gRPC reserveStock: orderId={}, items={}", orderId, items.size());
        var builder = ReserveStockRequest.newBuilder().setOrderId(orderId);
        items.forEach(i -> builder.addItems(ReserveItem.newBuilder()
                .setProductId(i.productId())   // String after proto migration
                .setQuantity(i.quantity())
                .build()));
        try {
            ReserveStockResponse resp = deadlined().reserveStock(builder.build());
            if (!resp.getSuccess()) {
                throw new InsufficientStockException(
                        "Insufficient stock for order " + orderId + ": " + resp.getErrorMessage());
            }
            return resp.getReservationId();
        } catch (StatusRuntimeException e) {
            log.error("gRPC reserveStock transport error: {} - {}", e.getStatus().getCode(), e.getMessage());
            throw new ServiceUnavailableException("inventory-service");
        }
    }

    public void confirmReservation(String reservationId) {
        log.debug("gRPC confirmReservation: {}", reservationId);
        try {
            deadlined().confirmReservation(ConfirmReservationRequest.newBuilder()
                    .setReservationId(reservationId).build());
        } catch (StatusRuntimeException e) {
            log.error("gRPC confirmReservation failed ({}): {}", reservationId, e.getMessage());
            throw new ServiceUnavailableException("inventory-service");
        }
    }

    /** Compensation path — idempotent on server; swallow transport failures. */
    public void releaseStock(String reservationId) {
        log.debug("gRPC releaseStock: {}", reservationId);
        try {
            deadlined().releaseStock(ReleaseStockRequest.newBuilder()
                    .setReservationId(reservationId).build());
        } catch (StatusRuntimeException e) {
            log.warn("gRPC releaseStock({}) failed: {} - {}",
                    reservationId, e.getStatus().getCode(), e.getMessage());
        }
    }

    public boolean checkStock(String productId, int quantity) {
        try {
            CheckStockResponse resp = deadlined().checkStock(CheckStockRequest.newBuilder()
                    .setProductId(productId).setQuantity(quantity).build());
            return resp.getAvailable();
        } catch (StatusRuntimeException e) {
            log.warn("gRPC checkStock({}, {}) failed: {}", productId, quantity, e.getMessage());
            return false;
        }
    }

    /** Input record so saga callers don't depend on generated proto types. */
    public record ReserveItemInput(String productId, int quantity) {}
}
