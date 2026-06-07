package com.hieu.inventory_service.service;

import com.hieu.inventory_service.dto.*;
import com.hieu.inventory_service.entity.InventoryEntity;
import com.hieu.inventory_service.entity.StockMovement;
import com.hieu.inventory_service.entity.StockReservationRecord;
import com.hieu.inventory_service.entity.StockReservationRecord.ReservationStatus;
import com.hieu.inventory_service.exception.InsufficientStockException;
import com.hieu.inventory_service.exception.InventoryNotFoundException;
import com.hieu.inventory_service.kafka.LowStockEventPublisher;
import com.hieu.inventory_service.repository.InventoryRepository;
import com.hieu.inventory_service.repository.StockMovementRepository;
import com.hieu.inventory_service.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Core business logic for inventory management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementRepository movementRepository;
    private final StockRedisService redisService;
    private final LowStockEventPublisher lowStockPublisher;
    private final ObjectMapper objectMapper;
    /**
     * Self-injection (lazy to break circular ref). Calling `this.doReserveDb()` bypasses
     * Spring's @Transactional/@Retryable proxy because internal calls don't go through it;
     * routing via the proxy bean re-enables the transactional advice.
     *
     * Constructor injection không khả thi vì gây vòng phụ thuộc tự thân.
     * @Lazy giải quyết được — đây là pattern chính thức được Spring documentation
     * khuyến nghị cho trường hợp gọi proxied-method nội bộ.
     */
    @SuppressWarnings("java:S6813")
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private InventoryService self;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public InventoryDTO create(String productId, String sku, Integer quantity, Integer minStockLevel) {
        var entity = InventoryEntity.builder()
            .productId(productId)
            .sku(sku)
            .quantity(quantity)
            .reservedQuantity(0)
            .minStockLevel(minStockLevel != null ? minStockLevel : 10)
            .build();
        var saved = inventoryRepository.save(entity);
        redisService.setStock(productId, saved.getAvailableQuantity());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public InventoryDTO getByProductId(String productId) {
        return inventoryRepository.findByProductId(productId)
            .map(this::toDTO)
            .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public InventoryDTO getBySku(String sku) {
        return inventoryRepository.findBySku(sku)
            .map(this::toDTO)
            .orElseThrow(() -> new InventoryNotFoundException("SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public PageDTO<InventoryDTO> getAll(int page, int size) {
        Page<InventoryEntity> p = inventoryRepository.findAll(PageRequest.of(page, size));
        return new PageDTO<>(
            p.getContent().stream().map(this::toDTO).toList(),
            p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public InventoryDTO adjustStock(String productId, int delta) {
        return adjustStock(productId, delta, null, null);
    }

    /**
     * Adjust stock and write an audit row to {@code stock_movements}. Reasons:
     *  - positive delta → admin restocked (or initial seed)
     *  - negative delta → admin removed (shrinkage, return-to-vendor, etc.)
     * Caller may supply an actor (admin userId) and free-text note that show up
     * in the history view.
     */
    @Transactional
    public InventoryDTO adjustStock(String productId, int delta, String actor, String note) {
        var inventories = inventoryRepository.findAllByProductIdInWithLock(List.of(productId));
        var entity = inventories.stream().findFirst()
            .orElseThrow(() -> new InventoryNotFoundException(productId));
        int before = entity.getQuantity();
        if (delta > 0) {
            entity.addStock(delta);
        } else if (delta < 0) {
            // Negative delta = shrink total quantity; guard against going below reserved.
            int reduction = -delta;
            if (entity.getQuantity() - reduction < entity.getReservedQuantity()) {
                throw new IllegalArgumentException(
                    "Cannot reduce quantity below reserved level: quantity=" + entity.getQuantity()
                    + " reserved=" + entity.getReservedQuantity() + " reduction=" + reduction);
            }
            entity.subtractStock(reduction);
        }
        var saved = inventoryRepository.save(entity);
        // Invalidate (don't SET) — concurrent reserves may have already deducted Redis;
        // next reserve will cache-miss and seed from DB under lock.
        redisService.invalidate(productId);

        movementRepository.save(StockMovement.builder()
                .productId(saved.getProductId())
                .sku(saved.getSku())
                .delta(delta)
                .quantityBefore(before)
                .quantityAfter(saved.getQuantity())
                .reservedAfter(saved.getReservedQuantity())
                .reason(StockMovement.Reason.ADJUST)
                .actor(actor != null ? actor : "ADMIN")
                .note(note)
                .build());

        return toDTO(saved);
    }

    /**
     * History query for the admin UI. Cursor-less — page+size only; consumers
     * paginate via `?page=&size=`. Optional filter by SKU narrows to a single
     * inventory row.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<StockMovementDTO> history(
            String productId, String sku, int page, int size) {
        var pageReq = org.springframework.data.domain.PageRequest.of(page, Math.min(size, 200));
        org.springframework.data.domain.Page<StockMovement> p;
        if (sku != null && !sku.isBlank()) {
            p = movementRepository.findBySkuOrderByCreatedAtDesc(sku, pageReq);
        } else if (productId != null) {
            p = movementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageReq);
        } else {
            p = movementRepository.findAllByOrderByCreatedAtDesc(pageReq);
        }
        return p.map(StockMovementDTO::from);
    }

    // -------------------------------------------------------------------------
    // Reserve
    // -------------------------------------------------------------------------

    public ReservationResult reserveStock(ReservationRequest request) {
        // Idempotency — first read is cheap dedup, authoritative check happens inside
        // doReserveDb (under unique constraint) to close the TOCTOU window between
        // two concurrent requests with the same orderId.
        var existing = reservationRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            log.info("Idempotent reserve: orderId={} status={}", request.orderId(), existing.get().getStatus());
            return ReservationResult.success(request.orderId());
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        for (var item : request.items()) {
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive for productId=" + item.productId());
            }
        }

        var itemMap = request.items().stream()
            .collect(Collectors.toMap(
                ReservationRequest.ReservationItem::productId,
                ReservationRequest.ReservationItem::quantity));

        // Atomic Redis check — runs exactly once (not inside @Retryable)
        int redisResult = redisService.reserveStockAtomically(itemMap);
        if (redisResult == -1) {
            log.info("Redis cache miss for order {}, seeding from DB", request.orderId());
            // Seed under its own transaction (PESSIMISTIC lock requires active tx) and
            // use SET-if-absent to avoid overwriting a concurrent reserve's deduction.
            self.seedRedisFromDb(itemMap.keySet().stream().toList());
            redisResult = redisService.reserveStockAtomically(itemMap);
        }
        if (redisResult == 0) {
            throw new InsufficientStockException("Insufficient stock for order " + request.orderId());
        }
        // Track whether Redis was actually decremented (result==1). If the second
        // reserveStockAtomically still returns -1 (persistent cache miss), Redis was
        // never decremented — calling releaseStockBatch in that case would add phantom
        // stock and cause Redis to drift above the DB value (oversell window).
        final boolean redisDecremented = (redisResult == 1);

        // DB part retried on optimistic lock conflict; Redis NOT re-executed on retry.
        // Route through self-proxy so @Transactional + @Retryable advice fires.
        // ALL non-success paths must release this thread's Redis deduction. @Recover
        // re-throws RetryExhaustedException so we hit the catch block here.
        try {
            return self.doReserveDb(request.orderId(), itemMap);
        } catch (DuplicateReservationException dup) {
            // Concurrent same-orderId race: BOTH threads independently deducted Redis,
            // but only the winner persisted to DB. The loser (us) must release its
            // Redis deduction or Redis drifts permanently below DB.
            log.info("Idempotent reserve (concurrent loser): orderId={}", dup.getOrderId());
            if (redisDecremented) redisService.releaseStockBatch(itemMap);
            return ReservationResult.success(dup.getOrderId());
        } catch (ReserveRetryExhaustedException exhausted) {
            // 3 optimistic-lock retries failed → release our Redis deduction.
            if (redisDecremented) redisService.releaseStockBatch(itemMap);
            return ReservationResult.failure("stock conflict, retry later");
        } catch (Exception e) {
            if (redisDecremented) redisService.releaseStockBatch(itemMap);
            throw e;
        }
    }

    /**
     * Seeds Redis from DB under a pessimistic lock. Uses SET-if-absent so a concurrent
     * reserve that has already seeded (and possibly deducted) the key wins — avoids
     * the load-then-set race where one thread overwrites another's deduction.
     */
    /**
     * Not readOnly — PESSIMISTIC_WRITE lock requires a writable tx (Postgres rejects
     * SELECT FOR UPDATE inside SET TRANSACTION READ ONLY). The tx still does no writes;
     * it just provides the lock scope.
     */
    @Transactional
    public void seedRedisFromDb(List<String> productIds) {
        var inventories = inventoryRepository.findAllByProductIdInWithLock(productIds);
        for (var inv : inventories) {
            redisService.setStockIfAbsent(inv.getProductId(), inv.getAvailableQuantity());
        }
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public ReservationResult doReserveDb(String orderId, Map<String, Integer> itemMap) {
        // Authoritative idempotency check: catch concurrent same-orderId via the
        // unique constraint on stock_reservations.order_id. The findByOrderId at the
        // start of reserveStock is a fast-path; this catch block closes the race.
        try {
            var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());
            var invMap = inventories.stream()
                .collect(Collectors.toMap(InventoryEntity::getProductId, Function.identity()));

            for (var entry : itemMap.entrySet()) {
                var inv = invMap.get(entry.getKey());
                if (inv == null) throw new InventoryNotFoundException(entry.getKey());
                inv.reserve(entry.getValue());
            }
            inventoryRepository.saveAll(inventories);

            var record = StockReservationRecord.builder()
                .orderId(orderId)
                .items(serializeItems(itemMap))
                .status(ReservationStatus.ACTIVE)
                .build();
            reservationRepository.saveAndFlush(record);

            // Publish low-stock events
            inventories.forEach(inv -> {
                if (inv.isLowStock()) lowStockPublisher.publishIfLowStock(inv);
            });

            return ReservationResult.success(orderId);
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // Concurrent reservation with same orderId won the unique-constraint race.
            // Treat as idempotent success — the caller will rollback Redis (which is wrong
            // here since the winning request also deducted Redis once). Return a sentinel
            // success but signal "already exists" so reserveStock skips Redis rollback.
            log.info("Concurrent same-orderId reserve detected (idempotent): orderId={}", orderId);
            throw new DuplicateReservationException(orderId);
        }
    }

    @Recover
    public ReservationResult recoverReserve(ObjectOptimisticLockingFailureException ex, String orderId, Map<String, Integer> itemMap) {
        log.warn("Reserve stock DB failed after retries for order {}: {}", orderId, ex.getMessage());
        // Re-throw so reserveStock's catch block releases the Redis deduction. Returning
        // failure here would mark the retry as "handled" and swallow the exception →
        // Redis would leak deducted stock forever (no DB row, no rollback path).
        throw new ReserveRetryExhaustedException(orderId, ex);
    }

    // -------------------------------------------------------------------------
    // Confirm
    // -------------------------------------------------------------------------

    @Transactional
    public ReservationResult confirmReservation(String orderId) {
        // Lock the reservation row first to serialize concurrent confirm/release
        // on the same orderId. Without this, both threads see ACTIVE and double-apply.
        var record = reservationRepository.findByOrderIdForUpdate(orderId)
            .orElse(null);
        if (record == null || record.getStatus() != ReservationStatus.ACTIVE) {
            log.info("Confirm idempotent: orderId={}", orderId);
            return ReservationResult.success(orderId);
        }

        var itemMap = deserializeItems(record.getItems());
        var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());
        var invMap = inventories.stream()
            .collect(Collectors.toMap(InventoryEntity::getProductId, Function.identity()));

        for (var entry : itemMap.entrySet()) {
            var inv = invMap.get(entry.getKey());
            if (inv != null) inv.confirmReservation(entry.getValue());
        }
        inventoryRepository.saveAll(inventories);

        record.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(record);

        inventories.forEach(inv -> {
            if (inv.isLowStock()) lowStockPublisher.publishIfLowStock(inv);
        });

        return ReservationResult.success(orderId);
    }

    // -------------------------------------------------------------------------
    // Release
    // -------------------------------------------------------------------------

    @Transactional
    public ReservationResult releaseReservation(String orderId) {
        // Same locking discipline as confirmReservation — prevents double-release.
        var record = reservationRepository.findByOrderIdForUpdate(orderId)
            .orElse(null);
        if (record == null || record.getStatus() != ReservationStatus.ACTIVE) {
            log.info("Release idempotent: orderId={}", orderId);
            return ReservationResult.success(orderId);
        }

        // Mark RELEASED first to prevent double-release
        record.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(record);

        var itemMap = deserializeItems(record.getItems());
        var inventories = inventoryRepository.findAllByProductIdInWithLock(itemMap.keySet().stream().toList());

        for (var inv : inventories) {
            var qty = itemMap.get(inv.getProductId());
            if (qty != null) {
                inv.releaseReservation(qty);
            }
        }
        inventoryRepository.saveAll(inventories);

        // Redis must only be incremented AFTER the DB commit succeeds. If the tx
        // rollbacks after this point, the AFTER_COMMIT callback simply does not fire —
        // Redis stays in sync. Otherwise Redis > DB drift on rollback → oversell risk.
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() {
                    itemMap.forEach(redisService::releaseStock);
                }
            });

        return ReservationResult.success(orderId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String serializeItems(Map<String, Integer> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize items", e);
        }
    }

    /**
     * JSON-deserialise {@code items} column. Keys are String UUIDs (productId).
     */
    private Map<String, Integer> deserializeItems(String json) {
        try {
            return objectMapper.readValue(json,
                new tools.jackson.core.type.TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize items: " + json, e);
        }
    }

    public InventoryDTO toDTO(InventoryEntity e) {
        return InventoryDTO.builder()
            .id(e.getId())
            .productId(e.getProductId())
            .sku(e.getSku())
            .quantity(e.getQuantity())
            .reservedQuantity(e.getReservedQuantity())
            .availableQuantity(e.getAvailableQuantity())
            .minStockLevel(e.getMinStockLevel())
            .lowStock(e.isLowStock())
            .lastUpdated(e.getLastUpdated())
            .version(e.getVersion())
            .build();
    }
}
