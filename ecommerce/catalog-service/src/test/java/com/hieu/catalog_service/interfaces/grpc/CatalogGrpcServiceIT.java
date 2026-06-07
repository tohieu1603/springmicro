package com.hieu.catalog_service.interfaces.grpc;

import com.hieu.catalog_service.AbstractIntegrationTest;
import com.hieu.catalog_service.application.command.product.CreateProductCommand;
import com.hieu.catalog_service.application.handler.product.CreateProductHandler;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogGrpcService — integration tests")
class CatalogGrpcServiceIT extends AbstractIntegrationTest {

    @Autowired CatalogGrpcService grpcService;
    @Autowired CreateProductHandler createProductHandler;

    private String existingSku;

    @BeforeEach
    void seedProduct() {
        existingSku = "SKU-GRPC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var variant = new CreateProductCommand.VariantCmd(
                existingSku,
                new BigDecimal("299.99"),
                new BigDecimal("150.00"),
                null,
                null,
                new BigDecimal("1.0"),
                50,
                List.of()
        );
        createProductHandler.handle(new CreateProductCommand(
                "gRPC Test Product " + existingSku, "desc", null, "Brand",
                null, List.of(), null, null, null,
                List.of(variant), false, "test-user"
        ));
    }

    // ── getVariantBySku ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getVariantBySku_existing_returnsFound — SKU tồn tại → found=true")
    void getVariantBySku_existing_returnsFound() {
        var resp = callGetVariantBySku(existingSku);

        assertThat(resp.getFound()).isTrue();
        assertThat(resp.getVariant().getSku()).isEqualTo(existingSku);
    }

    @Test
    @DisplayName("getVariantBySku_notFound_returnsFound_false — SKU không tồn tại → found=false, không throw gRPC error")
    void getVariantBySku_notFound_returnsFound_false() {
        var resp = callGetVariantBySku("SKU-DOES-NOT-EXIST-XYZ");

        assertThat(resp.getFound()).isFalse();
    }

    // ── checkStock ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkStock_returnsAvailableTrue_whenQtyEnough — tồn kho đủ → available=true")
    void checkStock_returnsAvailableTrue_whenQtyEnough() {
        var req = CheckStockRequest.newBuilder()
                .setSku(existingSku)
                .setRequested(10)
                .build();

        CheckStockResponse resp = callCheckStock(req);

        assertThat(resp.getAvailable()).isTrue();
        assertThat(resp.getQuantity()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("checkStock_returnsAvailableFalse_whenInsufficient — yêu cầu vượt tồn kho → available=false")
    void checkStock_returnsAvailableFalse_whenInsufficient() {
        var req = CheckStockRequest.newBuilder()
                .setSku(existingSku)
                .setRequested(9999)
                .build();

        CheckStockResponse resp = callCheckStock(req);

        assertThat(resp.getAvailable()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GetVariantBySkuResponse callGetVariantBySku(String sku) {
        var req = GetVariantBySkuRequest.newBuilder().setSku(sku).build();
        AtomicReference<GetVariantBySkuResponse> holder = new AtomicReference<>();
        grpcService.getVariantBySku(req, captureObserver(holder));
        return holder.get();
    }

    private CheckStockResponse callCheckStock(CheckStockRequest req) {
        AtomicReference<CheckStockResponse> holder = new AtomicReference<>();
        grpcService.checkStock(req, captureObserver(holder));
        return holder.get();
    }

    private static <T> StreamObserver<T> captureObserver(AtomicReference<T> holder) {
        return new StreamObserver<T>() {
            @Override public void onNext(T value)      { holder.set(value); }
            @Override public void onError(Throwable t) { throw new AssertionError("gRPC error: " + t.getMessage(), t); }
            @Override public void onCompleted()        { /* no-op */ }
        };
    }
}
