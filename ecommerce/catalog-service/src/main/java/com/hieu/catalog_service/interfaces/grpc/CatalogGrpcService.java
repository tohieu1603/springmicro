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
import com.hieu.catalog_service.interfaces.grpc.proto.CatalogServiceGrpc;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.Product;
import com.hieu.catalog_service.interfaces.grpc.proto.Variant;
import com.hieu.catalog_service.interfaces.grpc.proto.VariantAttr;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.math.BigDecimal;

/**
 * gRPC facade over the application layer — sibling microservices (order, cart, inventory)
 * call these RPCs instead of HTTP to keep the hot path cheap.
 *
 * <p>Follows the same {@code found=false} / {@code available=false} convention as
 * auth-service: lookups never return NOT_FOUND gRPC status for expected-missing rows,
 * just a flag on the response.
 */
@GrpcService
@RequiredArgsConstructor
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcService.class);

    private final QueryHandler<GetProductByIdQuery, ProductDTO> getProductById;
    private final QueryHandler<GetVariantBySkuQuery, VariantDTO> getVariantBySku;
    private final QueryHandler<CheckStockQuery, VariantDTO> checkStock;

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<GetProductResponse> observer) {
        GetProductResponse.Builder reply = GetProductResponse.newBuilder();
        try {
            ProductDTO dto = getProductById.handle(new GetProductByIdQuery(request.getProductId()));
            reply.setFound(true).setProduct(toProto(dto));
        } catch (ProductNotFoundException nf) {
            reply.setFound(false);
        } catch (Exception e) {
            log.warn("gRPC getProduct failed: {}", e.getMessage());
            reply.setFound(false);
        }
        observer.onNext(reply.build());
        observer.onCompleted();
    }

    @Override
    public void getVariantBySku(GetVariantBySkuRequest request, StreamObserver<GetVariantBySkuResponse> observer) {
        GetVariantBySkuResponse.Builder reply = GetVariantBySkuResponse.newBuilder();
        try {
            VariantDTO dto = getVariantBySku.handle(new GetVariantBySkuQuery(request.getSku()));
            reply.setFound(true).setVariant(toProto(dto));
        } catch (VariantNotFoundException nf) {
            reply.setFound(false);
        } catch (Exception e) {
            log.error("gRPC getVariantBySku failed for sku={}", request.getSku(), e);
            reply.clear().setFound(false);
        }
        observer.onNext(reply.build());
        observer.onCompleted();
    }

    @Override
    public void checkStock(CheckStockRequest request, StreamObserver<CheckStockResponse> observer) {
        CheckStockResponse.Builder reply = CheckStockResponse.newBuilder();
        try {
            // Single fetch — CheckStockHandler returns VariantDTO; available derived here
            VariantDTO dto = checkStock.handle(new CheckStockQuery(request.getSku(), request.getRequested()));
            boolean available = dto.quantity() >= request.getRequested();
            reply.setAvailable(available).setQuantity(dto.quantity()).setStatus(dto.status());
        } catch (VariantNotFoundException nf) {
            reply.setAvailable(false).setQuantity(0).setStatus("");
        } catch (Exception e) {
            log.warn("gRPC checkStock failed: {}", e.getMessage());
            reply.setAvailable(false).setQuantity(0).setStatus("");
        }
        observer.onNext(reply.build());
        observer.onCompleted();
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private Product toProto(ProductDTO p) {
        Product.Builder b = Product.newBuilder()
            .setId(nz(p.id()))
            .setName(nz(p.name()))
            .setSlug(nz(p.slug()))
            .setDescription(nz(p.description()))
            .setCategoryId(nz(p.categoryId()))
            .setBrand(nz(p.brand()))
            .setThumbnail(nz(p.thumbnail()))
            .setStatus(nz(p.status()));
        p.variants().forEach(v -> b.addVariants(toProto(v)));
        return b.build();
    }

    private Variant toProto(VariantDTO v) {
        Variant.Builder b = Variant.newBuilder()
            .setId(nz(v.id()))
            .setProductId(nz(v.productId()))
            .setSku(nz(v.sku()))
            .setPrice(bd(v.price()))
            .setSalePrice(bd(v.salePrice()))
            .setQuantity(v.quantity())
            .setStatus(nz(v.status()));
        if (v.attrs() != null) v.attrs().forEach(a -> b.addAttrs(toProto(a)));
        return b.build();
    }

    private VariantAttr toProto(VariantAttrDTO a) {
        return VariantAttr.newBuilder()
            .setAttrId(nz(a.attrId()))
            .setAttrCode(nz(a.attrCode()))
            .setAttrName(nz(a.attrName()))
            .setValId(nz(a.valId()))
            .setValText(nz(a.valText()))
            .build();
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String bd(BigDecimal b) { return b == null ? "" : b.toPlainString(); }
}
