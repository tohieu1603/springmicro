package com.hieu.inventory_service.grpc;

import com.hieu.inventory_service.dto.ReservationRequest;
import com.hieu.inventory_service.exception.InsufficientStockException;
import com.hieu.inventory_service.exception.InventoryNotFoundException;
import com.hieu.inventory_service.interfaces.grpc.proto.*;
import com.hieu.inventory_service.service.InventoryService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

/**
 * gRPC server implementation — delegates to {@link InventoryService}.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryService inventoryService;

    @Override
    public void checkStock(CheckStockRequest req, StreamObserver<CheckStockResponse> obs) {
        try {
            var dto = inventoryService.getByProductId(req.getProductId());
            var resp = CheckStockResponse.newBuilder()
                .setAvailable(dto.getAvailableQuantity() >= req.getQuantity())
                .setAvailableQuantity(dto.getAvailableQuantity())
                .build();
            obs.onNext(resp);
            obs.onCompleted();
        } catch (InventoryNotFoundException e) {
            obs.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            obs.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void reserveStock(ReserveStockRequest req, StreamObserver<ReserveStockResponse> obs) {
        try {
            if (req.getOrderId() == null || req.getOrderId().isBlank()) {
                obs.onError(Status.INVALID_ARGUMENT.withDescription("orderId must not be blank").asRuntimeException());
                return;
            }
            if (req.getItemsList().isEmpty()) {
                obs.onError(Status.INVALID_ARGUMENT.withDescription("items must not be empty").asRuntimeException());
                return;
            }
            var items = req.getItemsList().stream()
                .map(i -> new ReservationRequest.ReservationItem(i.getProductId(), i.getQuantity()))
                .toList();
            var result = inventoryService.reserveStock(new ReservationRequest(req.getOrderId(), items));
            obs.onNext(ReserveStockResponse.newBuilder()
                .setSuccess(result.success())
                .setReservationId(result.reservationId() != null ? result.reservationId() : "")
                .setErrorMessage(result.errorMessage() != null ? result.errorMessage() : "")
                .build());
            obs.onCompleted();
        } catch (InsufficientStockException e) {
            obs.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (InventoryNotFoundException e) {
            obs.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            obs.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void confirmReservation(ConfirmReservationRequest req, StreamObserver<ConfirmReservationResponse> obs) {
        try {
            if (req.getReservationId() == null || req.getReservationId().isBlank()) {
                obs.onError(Status.INVALID_ARGUMENT.withDescription("reservationId must not be blank").asRuntimeException());
                return;
            }
            var result = inventoryService.confirmReservation(req.getReservationId());
            obs.onNext(ConfirmReservationResponse.newBuilder().setSuccess(result.success()).build());
            obs.onCompleted();
        } catch (InventoryNotFoundException e) {
            obs.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            obs.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            obs.onError(Status.INTERNAL.asRuntimeException());
        }
    }

    @Override
    public void releaseStock(ReleaseStockRequest req, StreamObserver<ReleaseStockResponse> obs) {
        try {
            if (req.getReservationId() == null || req.getReservationId().isBlank()) {
                obs.onError(Status.INVALID_ARGUMENT.withDescription("reservationId must not be blank").asRuntimeException());
                return;
            }
            var result = inventoryService.releaseReservation(req.getReservationId());
            obs.onNext(ReleaseStockResponse.newBuilder().setSuccess(result.success()).build());
            obs.onCompleted();
        } catch (InventoryNotFoundException e) {
            obs.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            obs.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            obs.onError(Status.INTERNAL.asRuntimeException());
        }
    }
}
