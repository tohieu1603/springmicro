package com.hieu.catalog_service.interfaces.grpc;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.dto.VariantAttrDTO;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.query.product.GetProductByIdQuery;
import com.hieu.catalog_service.application.query.variant.CheckStockQuery;
import com.hieu.catalog_service.application.query.variant.GetVariantBySkuQuery;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.exception.VariantNotFoundException;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for the gRPC facade's DTO → protobuf mapping. Mocks the three application
 * QueryHandlers and captures the StreamObserver output — no gRPC server, Spring, or DB.
 * Covers: getProduct found/not-found, getVariantBySku found/not-found, checkStock
 * available vs insufficient, and the null-coalescing helpers (nz / bd) on nullable fields.
 */
@DisplayName("CatalogGrpcService — unit (proto mapping)")
class CatalogGrpcServiceTest {

    // Same erased type (QueryHandler) for all three collaborators, so we wire them via the
    // constructor explicitly rather than relying on by-name @InjectMocks resolution.
    @SuppressWarnings("unchecked")
    private final QueryHandler<GetProductByIdQuery, ProductDTO> getProductById = mock(QueryHandler.class);
    @SuppressWarnings("unchecked")
    private final QueryHandler<GetVariantBySkuQuery, VariantDTO> getVariantBySku = mock(QueryHandler.class);
    @SuppressWarnings("unchecked")
    private final QueryHandler<CheckStockQuery, VariantDTO> checkStock = mock(QueryHandler.class);

    private CatalogGrpcService service;

    @BeforeEach
    void setUp() {
        service = new CatalogGrpcService(getProductById, getVariantBySku, checkStock);
    }

    private static <T> StreamObserver<T> capture(AtomicReference<T> holder) {
        return new StreamObserver<>() {
            @Override public void onNext(T value)      { holder.set(value); }
            @Override public void onError(Throwable t) { throw new AssertionError("unexpected gRPC error", t); }
            @Override public void onCompleted()        { /* no-op */ }
        };
    }

    private static VariantAttrDTO attr() {
        return new VariantAttrDTO("1", "7", "COLOR", "Color", "3", "Red");
    }

    private static VariantDTO variant() {
        return new VariantDTO("10", "1", "SKU-1", new BigDecimal("19.99"), null,
            new BigDecimal("15.00"), new BigDecimal("15.00"), "img.png",
            new BigDecimal("1.5"), 8, "ACTIVE", true, List.of(attr()));
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("found → found=true and product (+nested variant/attr) mapped to proto")
        void found() {
            var dto = new ProductDTO("1", "Tee", "tee", "desc", "99", "Brand", "thumb.png",
                List.of("a.png"), "ACTIVE", null, null, null,
                new BigDecimal("15.00"), new BigDecimal("19.99"), 8, true,
                List.of(variant()), null, null, "creator", "updater", 2L);
            when(getProductById.handle(new GetProductByIdQuery("1"))).thenReturn(dto);

            var holder = new AtomicReference<GetProductResponse>();
            service.getProduct(GetProductRequest.newBuilder().setProductId("1").build(), capture(holder));

            GetProductResponse resp = holder.get();
            assertThat(resp.getFound()).isTrue();
            assertThat(resp.getProduct().getId()).isEqualTo("1");
            assertThat(resp.getProduct().getName()).isEqualTo("Tee");
            assertThat(resp.getProduct().getStatus()).isEqualTo("ACTIVE");
            assertThat(resp.getProduct().getVariantsCount()).isEqualTo(1);
            assertThat(resp.getProduct().getVariants(0).getSku()).isEqualTo("SKU-1");
            assertThat(resp.getProduct().getVariants(0).getPrice()).isEqualTo("19.99");
            assertThat(resp.getProduct().getVariants(0).getAttrsCount()).isEqualTo(1);
            assertThat(resp.getProduct().getVariants(0).getAttrs(0).getAttrCode()).isEqualTo("COLOR");
        }

        @Test
        @DisplayName("null categoryId and null nullable strings map to empty string")
        void nullFieldsCoalesced() {
            var dto = new ProductDTO("2", null, null, null, null, null, null,
                List.of(), null, null, null, null, null, null, 0, false,
                List.of(), null, null, null, null, null);
            when(getProductById.handle(new GetProductByIdQuery("2"))).thenReturn(dto);

            var holder = new AtomicReference<GetProductResponse>();
            service.getProduct(GetProductRequest.newBuilder().setProductId("2").build(), capture(holder));

            var product = holder.get().getProduct();
            assertThat(product.getCategoryId()).isEmpty();
            assertThat(product.getName()).isEmpty();
            assertThat(product.getStatus()).isEmpty();
        }

        @Test
        @DisplayName("ProductNotFoundException → found=false (no gRPC error)")
        void notFound() {
            when(getProductById.handle(new GetProductByIdQuery("404")))
                .thenThrow(new ProductNotFoundException("404"));

            var holder = new AtomicReference<GetProductResponse>();
            service.getProduct(GetProductRequest.newBuilder().setProductId("404").build(), capture(holder));

            assertThat(holder.get().getFound()).isFalse();
        }

        @Test
        @DisplayName("unexpected exception → swallowed, found=false")
        void unexpectedError() {
            when(getProductById.handle(new GetProductByIdQuery("5")))
                .thenThrow(new RuntimeException("boom"));

            var holder = new AtomicReference<GetProductResponse>();
            service.getProduct(GetProductRequest.newBuilder().setProductId("5").build(), capture(holder));

            assertThat(holder.get().getFound()).isFalse();
        }
    }

    @Nested
    @DisplayName("getVariantBySku")
    class GetVariantBySku {

        @Test
        @DisplayName("found → found=true and variant mapped, salePrice serialised as plain string")
        void found() {
            when(getVariantBySku.handle(new GetVariantBySkuQuery("SKU-1"))).thenReturn(variant());

            var holder = new AtomicReference<GetVariantBySkuResponse>();
            service.getVariantBySku(GetVariantBySkuRequest.newBuilder().setSku("SKU-1").build(), capture(holder));

            GetVariantBySkuResponse resp = holder.get();
            assertThat(resp.getFound()).isTrue();
            assertThat(resp.getVariant().getId()).isEqualTo("10");
            assertThat(resp.getVariant().getProductId()).isEqualTo("1");
            assertThat(resp.getVariant().getSku()).isEqualTo("SKU-1");
            assertThat(resp.getVariant().getPrice()).isEqualTo("19.99");
            assertThat(resp.getVariant().getSalePrice()).isEqualTo("15.00");
            assertThat(resp.getVariant().getQuantity()).isEqualTo(8);
        }

        @Test
        @DisplayName("null id/productId/salePrice coalesce to empty string")
        void nullFieldsCoalesced() {
            var dto = new VariantDTO(null, null, "SKU-X", new BigDecimal("5.00"), null, null,
                new BigDecimal("5.00"), null, null, 0, "OUT_OF_STOCK", false, null);
            when(getVariantBySku.handle(new GetVariantBySkuQuery("SKU-X"))).thenReturn(dto);

            var holder = new AtomicReference<GetVariantBySkuResponse>();
            service.getVariantBySku(GetVariantBySkuRequest.newBuilder().setSku("SKU-X").build(), capture(holder));

            var v = holder.get().getVariant();
            assertThat(v.getId()).isEmpty();
            assertThat(v.getProductId()).isEmpty();
            assertThat(v.getSalePrice()).isEmpty();
            assertThat(v.getAttrsCount()).isZero();
        }

        @Test
        @DisplayName("VariantNotFoundException → found=false")
        void notFound() {
            when(getVariantBySku.handle(new GetVariantBySkuQuery("MISSING")))
                .thenThrow(VariantNotFoundException.bySku("MISSING"));

            var holder = new AtomicReference<GetVariantBySkuResponse>();
            service.getVariantBySku(GetVariantBySkuRequest.newBuilder().setSku("MISSING").build(), capture(holder));

            assertThat(holder.get().getFound()).isFalse();
        }
    }

    @Nested
    @DisplayName("checkStock")
    class CheckStock {

        @Test
        @DisplayName("quantity >= requested → available=true")
        void available() {
            when(checkStock.handle(new CheckStockQuery("SKU-1", 5))).thenReturn(variant());

            var holder = new AtomicReference<CheckStockResponse>();
            service.checkStock(CheckStockRequest.newBuilder().setSku("SKU-1").setRequested(5).build(), capture(holder));

            CheckStockResponse resp = holder.get();
            assertThat(resp.getAvailable()).isTrue();
            assertThat(resp.getQuantity()).isEqualTo(8);
            assertThat(resp.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("quantity < requested → available=false but quantity/status still reported")
        void insufficient() {
            when(checkStock.handle(new CheckStockQuery("SKU-1", 99))).thenReturn(variant());

            var holder = new AtomicReference<CheckStockResponse>();
            service.checkStock(CheckStockRequest.newBuilder().setSku("SKU-1").setRequested(99).build(), capture(holder));

            CheckStockResponse resp = holder.get();
            assertThat(resp.getAvailable()).isFalse();
            assertThat(resp.getQuantity()).isEqualTo(8);
        }

        @Test
        @DisplayName("VariantNotFoundException → available=false, quantity=0, status empty")
        void notFound() {
            when(checkStock.handle(new CheckStockQuery("MISSING", 1)))
                .thenThrow(VariantNotFoundException.bySku("MISSING"));

            var holder = new AtomicReference<CheckStockResponse>();
            service.checkStock(CheckStockRequest.newBuilder().setSku("MISSING").setRequested(1).build(), capture(holder));

            CheckStockResponse resp = holder.get();
            assertThat(resp.getAvailable()).isFalse();
            assertThat(resp.getQuantity()).isZero();
            assertThat(resp.getStatus()).isEmpty();
        }
    }
}
