package com.hieu.cart_service.service;

import com.hieu.cart_service.dto.AddToCartRequest;
import com.hieu.cart_service.dto.CartDTO;
import com.hieu.cart_service.dto.CartItemDTO;
import com.hieu.cart_service.dto.UpdateCartItemRequest;
import com.hieu.cart_service.entity.CartItem;
import com.hieu.cart_service.exception.CartItemNotFoundException;
import com.hieu.cart_service.grpc.client.CatalogGrpcClient;
import com.hieu.cart_service.redis.CartCacheService;
import com.hieu.cart_service.repository.CartItemRepository;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.Product;
import com.hieu.catalog_service.interfaces.grpc.proto.Variant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service for cart operations.
 * Coordinates repository, Redis cache and catalog gRPC client.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CartItemRepository cartItemRepository;
    private final CartCacheService cacheService;
    private final Optional<CatalogGrpcClient> catalogClient;

    // ── READ ─────────────────────────────────────────────────────────────────

    /**
     * Fetches cart for user; uses Redis cache first.
     * Revalidates each item via catalog gRPC and attaches warnings when unavailable.
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(String userId) {
        var cached = cacheService.getCart(userId);
        if (cached != null) {
            return cached;
        }
        var items = cartItemRepository.findAllByUserId(userId);
        var cart = buildCartDTO(userId, items, true);
        cacheService.putCart(userId, cart);
        return cart;
    }

    // ── WRITE ────────────────────────────────────────────────────────────────

    /**
     * Adds or updates a cart item with strict validation.
     *
     * Rejects with 404 if product/variant is gone, 409 if inactive or stock
     * insufficient. Idempotency via Redis-cached response.
     *
     * Two concurrent addItem() for the same (userId, variantId) collide on
     * @Version and Spring Retry replays the transaction up to 3 times so the
     * loser doesn't surface as a 500 to the user.
     */
    @org.springframework.retry.annotation.Retryable(
        retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public CartDTO addItem(String userId, AddToCartRequest req) {
        // Idempotency guard — if same key already produced a result, return it.
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var idem = cacheService.getIdempotentResult(req.idempotencyKey());
            if (idem != null) return idem;
        }

        // 1) Verify product + variant exist, ACTIVE, fetch authoritative metadata.
        ValidatedVariant valid = loadAndValidate(req.productId(), req.variantId());

        // 2) Stock check — incremental add must fit within remaining inventory.
        var existing = cartItemRepository.findByUserIdAndVariantId(userId, req.variantId());
        int currentQty = existing.map(CartItem::getQuantity).orElse(0);
        int targetQty = currentQty + req.quantity();
        if (targetQty > valid.variant.getQuantity()) {
            int remaining = Math.max(0, valid.variant.getQuantity() - currentQty);
            String msg = remaining == 0
                ? "Đã đạt giới hạn tồn kho. Trong giỏ hiện có " + currentQty + "/" + valid.variant.getQuantity() + " sản phẩm."
                : "Chỉ có thể thêm tối đa " + remaining + " sản phẩm (tồn kho " + valid.variant.getQuantity() + ").";
            throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
        }
        if (targetQty > 999) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mỗi loại sản phẩm tối đa 999 trong giỏ.");
        }

        // 3) Upsert.
        CartItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(targetQty);
        } else {
            item = CartItem.builder()
                .userId(userId)
                .variantId(req.variantId())
                .productId(valid.product.getId())
                .productName(valid.product.getName())
                .variantSku(valid.variant.getSku())
                .variantImage(valid.product.getThumbnail().isEmpty() ? null : valid.product.getThumbnail())
                .unitPrice(parseBD(valid.variant.getPrice()))
                .quantity(req.quantity())
                .build();
        }
        // Always refresh denormalised fields so cart reflects latest catalog state.
        item.setUnitPrice(parseBD(valid.variant.getPrice()));
        item.setVariantSku(valid.variant.getSku());
        item.setProductName(valid.product.getName());
        if (!valid.product.getThumbnail().isEmpty()) {
            item.setVariantImage(valid.product.getThumbnail());
        }

        cartItemRepository.save(item);
        // C2: evict only — next getCart rebuilds from DB avoiding a stale-read race.
        cacheService.evictCart(userId);

        var cart = buildCartDTO(userId, cartItemRepository.findAllByUserId(userId), false);
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            cacheService.putIdempotentResult(req.idempotencyKey(), cart);
        }
        return cart;
    }

    /**
     * Updates quantity of an existing cart item. quantity=0 means delete.
     * Re-validates stock + ACTIVE status against catalog so a stale tab can't
     * push the cart past current inventory.
     */
    @Transactional
    public CartDTO updateItem(String userId, String variantId, UpdateCartItemRequest req) {
        if (req.quantity() == 0) {
            return removeItem(userId, variantId);
        }
        var item = cartItemRepository.findByUserIdAndVariantId(userId, variantId)
            .orElseThrow(() -> new CartItemNotFoundException(userId, variantId));

        // Re-check catalog — the user may have left the tab open for hours.
        ValidatedVariant valid = loadAndValidate(item.getProductId(), variantId);
        if (req.quantity() > valid.variant.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Chỉ còn " + valid.variant.getQuantity() + " sản phẩm trong kho.");
        }
        if (req.quantity() > 999) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tối đa 999 sản phẩm cho mỗi loại.");
        }

        item.setQuantity(req.quantity());
        item.setUnitPrice(parseBD(valid.variant.getPrice()));
        cartItemRepository.save(item);
        cacheService.evictCart(userId);
        return refreshAndCache(userId);
    }

    /** Deletes a single cart item. */
    @Transactional
    public CartDTO removeItem(String userId, String variantId) {
        cartItemRepository.deleteByUserIdAndVariantId(userId, variantId);
        cacheService.evictCart(userId);
        return refreshAndCache(userId);
    }

    /** Clears the entire cart for a user. */
    @Transactional
    public void clearCart(String userId) {
        cartItemRepository.deleteAllByUserId(userId);
        cacheService.evictCart(userId);
    }

    // ── PACKAGE/INTERNAL ─────────────────────────────────────────────────────

    /** Called by Kafka consumer: remove items by productId and evict affected caches. */
    @Transactional
    public void removeItemsByProduct(String productId) {
        var affected = cartItemRepository.findUserIdsByProductId(productId);
        cartItemRepository.deleteAllByProductId(productId);
        affected.forEach(cacheService::evictCart);
        log.info("Removed cart items for productId={}, affected users={}", productId, affected.size());
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private record ValidatedVariant(Product product, Variant variant) {}

    /**
     * Round-trip to catalog for the freshest product + variant; reject with
     * the right HTTP code if anything has changed since the user's page loaded.
     */
    private ValidatedVariant loadAndValidate(String productId, String variantId) {
        if (catalogClient.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể kiểm tra tồn kho lúc này. Vui lòng thử lại.");
        }
        GetProductResponse resp = catalogClient.get().getProduct(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Catalog hiện không phản hồi. Vui lòng thử lại."));
        if (!resp.getFound()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Sản phẩm không còn tồn tại hoặc đã bị xoá.");
        }
        Product product = resp.getProduct();
        if (!STATUS_ACTIVE.equalsIgnoreCase(product.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Sản phẩm hiện không còn được bán.");
        }
        Variant variant = product.getVariantsList().stream()
            .filter(v -> variantId.equals(v.getId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Phân loại sản phẩm không còn tồn tại."));
        if (!STATUS_ACTIVE.equalsIgnoreCase(variant.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Phân loại sản phẩm đã ngưng kinh doanh.");
        }
        if (variant.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Phân loại sản phẩm đã hết hàng.");
        }
        return new ValidatedVariant(product, variant);
    }

    private CartDTO refreshAndCache(String userId) {
        var items = cartItemRepository.findAllByUserId(userId);
        var cart = buildCartDTO(userId, items, false);
        cacheService.putCart(userId, cart);
        return cart;
    }

    private CartDTO buildCartDTO(String userId, List<CartItem> items, boolean revalidate) {
        var warnings = new ArrayList<String>();
        var dtoItems = items.stream().map(item -> {
            String warning = null;
            if (revalidate) {
                warning = revalidateItem(item);
                if (warning != null) warnings.add(warning);
            }
            var subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemDTO(
                item.getId(), item.getProductId(), item.getProductName(),
                item.getVariantId(), item.getVariantSku(), item.getVariantImage(),
                item.getUnitPrice(), item.getQuantity(), subtotal,
                warning, item.getUpdatedAt());
        }).toList();

        var totalAmount = dtoItems.stream()
            .map(CartItemDTO::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDTO(userId, dtoItems, dtoItems.size(), totalAmount, warnings);
    }

    /** Returns a warning string if the item's variant is unavailable/price-changed, or null. */
    private String revalidateItem(CartItem item) {
        if (catalogClient.isEmpty()) return null;
        var respOpt = catalogClient.get().getVariantBySku(item.getVariantSku());
        if (respOpt.isEmpty()) return "Không kết nối được catalog (" + item.getVariantSku() + ")";
        var resp = respOpt.get();
        if (!resp.getFound()) return "Phân loại đã bị xoá (" + item.getVariantSku() + ")";
        var v = resp.getVariant();
        if (!STATUS_ACTIVE.equalsIgnoreCase(v.getStatus())) return "Phân loại đã ngưng bán (" + item.getVariantSku() + ")";
        if (v.getQuantity() < item.getQuantity()) {
            return "Tồn kho giảm còn " + v.getQuantity() + " (" + item.getVariantSku() + ")";
        }
        var catalogPrice = parseBD(v.getPrice());
        if (catalogPrice.compareTo(item.getUnitPrice()) != 0) {
            return "Giá đã thay đổi (" + item.getVariantSku() + "): " + item.getUnitPrice() + " → " + catalogPrice;
        }
        return null;
    }

    private static BigDecimal parseBD(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
