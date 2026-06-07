package com.hieu.order_service.interfaces.grpc;

import com.hieu.order_service.application.handler.order.GetOrderByIdInternalHandler;
import com.hieu.order_service.application.handler.order.GetOrderByNumberHandler;
import com.hieu.order_service.application.query.order.GetOrderByIdInternalQuery;
import com.hieu.order_service.application.query.order.GetOrderByNumberQuery;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.interfaces.grpc.proto.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

/** gRPC server for inter-service order lookups. */
@GrpcService
@RequiredArgsConstructor
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final GetOrderByIdInternalHandler getByIdHandler;
    private final GetOrderByNumberHandler getByNumberHandler;

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        try {
            var dto = getByIdHandler.handle(new GetOrderByIdInternalQuery(request.getOrderId())); // now String
            responseObserver.onNext(buildResponse(dto));
        } catch (OrderNotFoundException e) {
            responseObserver.onNext(GetOrderResponse.newBuilder().setFound(false).build());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getOrderByNumber(GetOrderByNumberRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        try {
            var dto = getByNumberHandler.handle(new GetOrderByNumberQuery(request.getOrderNumber(), null, true));
            responseObserver.onNext(buildResponse(dto));
        } catch (OrderNotFoundException e) {
            responseObserver.onNext(GetOrderResponse.newBuilder().setFound(false).build());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        responseObserver.onCompleted();
    }

    private GetOrderResponse buildResponse(com.hieu.order_service.application.dto.OrderDTO dto) {
        var builder = GetOrderResponse.newBuilder()
                .setFound(true)
                .setId(dto.id())
                .setOrderNumber(dto.orderNumber())
                .setUserId(dto.userId())
                .setStatus(dto.status())
                .setTotalAmount(dto.totalAmount().toPlainString())
                .setRecipientName(dto.recipientName())
                .setRecipientPhone(dto.recipientPhone())
                .setAddressLine(dto.street())
                .setWard(dto.ward())
                .setDistrict(dto.district())
                .setCity(dto.city());

        if (dto.items() != null) {
            dto.items().forEach(i -> builder.addItems(
                    OrderItemSnapshot.newBuilder()
                            .setVariantId(i.variantId() != null ? i.variantId() : "")
                            .setSku(i.variantSku() != null ? i.variantSku() : "")
                            .setQuantity(i.quantity())
                            .setUnitPrice(i.unitPrice().toPlainString())
                            .build()
            ));
        }
        return builder.build();
    }
}
