package com.hieu.flash_sale_service.service;

import com.hieu.flash_sale_service.dto.*;
import com.hieu.flash_sale_service.entity.FlashSaleJpaEntity;
import com.hieu.flash_sale_service.entity.FlashSaleParticipation;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import com.hieu.flash_sale_service.exception.*;
import com.hieu.flash_sale_service.kafka.*;
import com.hieu.flash_sale_service.repository.FlashSaleParticipationRepository;
import com.hieu.flash_sale_service.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Core application service for flash sale management. */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleApplicationService {

    private static final long MIN_DURATION_MINUTES = 5;

    private final FlashSaleRepository repository;
    private final FlashSaleParticipationRepository participationRepo;
    private final SlotRedisService slotRedisService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public FlashSaleDTO createSale(CreateFlashSaleRequest req) {
        var now = clock.instant();
        if (!req.startTime().isBefore(req.endTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        if (Duration.between(req.startTime(), req.endTime()).toMinutes() < MIN_DURATION_MINUTES) {
            throw new IllegalArgumentException("Sale duration must be at least " + MIN_DURATION_MINUTES + " minutes");
        }
        if (!req.startTime().isAfter(now)) {
            throw new IllegalArgumentException("startTime must be in the future");
        }
        if (req.salePrice().compareTo(req.originalPrice()) >= 0) {
            throw new IllegalArgumentException("salePrice must be less than originalPrice");
        }

        var entity = new FlashSaleJpaEntity();
        entity.setProductId(req.productId());
        entity.setProductName(req.productName());
        entity.setOriginalPrice(req.originalPrice());
        entity.setSalePrice(req.salePrice());
        entity.setTotalSlots(req.totalSlots());
        entity.setReservedSlots(0);
        entity.setMaxPerUser(req.maxPerUser());
        entity.setStartTime(req.startTime());
        entity.setEndTime(req.endTime());
        entity.setStatus(FlashSaleStatus.SCHEDULED);
        entity.setDescription(req.description());

        var saved = repository.save(entity);
        log.debug("Created flash sale id={} productId={}", saved.getId(), saved.getProductId());
        return toDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public FlashSaleDTO getSale(String id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public PageDTO<FlashSaleDTO> listActiveSales(int page, int size) {
        var pg = repository.findActiveSales(
                FlashSaleStatus.ACTIVE, clock.instant(), PageRequest.of(page, size));
        return toPageDTO(pg.map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public PageDTO<FlashSaleDTO> listAllSales(int page, int size) {
        return toPageDTO(repository.findAll(PageRequest.of(page, size)).map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(String id) {
        var entity = findById(id);
        var now = clock.instant();
        boolean statusActive = entity.getStatus() == FlashSaleStatus.ACTIVE;
        boolean withinWindow = !now.isBefore(entity.getStartTime()) && !now.isAfter(entity.getEndTime());

        Integer redisVal = slotRedisService.getRemaining(id);
        int remaining = redisVal != null
                ? Math.max(0, redisVal)
                : Math.max(0, entity.getTotalSlots() - entity.getReservedSlots());

        boolean available = remaining > 0 && statusActive && withinWindow;
        String reason = null;
        if (!available) {
            if (!statusActive)       reason = "Sale status is " + entity.getStatus();
            else if (!withinWindow)  reason = now.isBefore(entity.getStartTime()) ? "Sale not started yet" : "Sale has ended";
            else                     reason = "No slots remaining";
        }
        return new AvailabilityResponse(entity.getTotalSlots(), entity.getReservedSlots(), remaining, available, reason);
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    @Transactional
    public FlashSaleDTO activateSale(String id) {
        var entity = findById(id);
        transition(entity, FlashSaleStatus.ACTIVE);
        var saved = repository.save(entity);
        // Seed Redis counter
        slotRedisService.seed(id, saved.getTotalSlots() - saved.getReservedSlots());
        // Emit event
        eventPublisher.publishEvent(new FlashSaleStartedEvent(
                UUID.randomUUID().toString(), Instant.now(clock),
                saved.getId(), saved.getProductId(),
                saved.getStartTime(), saved.getEndTime(), saved.getTotalSlots()));
        return toDTO(saved);
    }

    @Transactional
    public FlashSaleDTO endSale(String id) {
        var entity = findById(id);
        transition(entity, FlashSaleStatus.ENDED);
        var saved = repository.save(entity);
        eventPublisher.publishEvent(new FlashSaleEndedEvent(
                UUID.randomUUID().toString(), Instant.now(clock),
                saved.getId(), saved.getProductId(), saved.getReservedSlots()));
        return toDTO(saved);
    }

    @Transactional
    public FlashSaleDTO cancelSale(String id) {
        var entity = findById(id);
        transition(entity, FlashSaleStatus.CANCELLED);
        return toDTO(repository.save(entity));
    }

    // -------------------------------------------------------------------------
    // Participate
    // -------------------------------------------------------------------------

    @Transactional
    public ParticipateResponse participate(String saleId, String userId, int quantity) {
        // 1. Pre-check with pessimistic read lock
        var sale = repository.findByIdWithReadLock(saleId)
                .orElseThrow(() -> new FlashSaleNotFoundException(saleId));
        var now = clock.instant();
        assertSaleActive(sale, saleId, now);

        // 2. Check user quota
        int alreadyClaimed = participationRepo.sumQuantityBySaleIdAndUserId(saleId, userId);
        if (alreadyClaimed + quantity > sale.getMaxPerUser()) {
            throw new UserQuotaExceededException(userId, saleId, sale.getMaxPerUser());
        }

        // 3. Redis atomic decrement via Lua script
        long result = slotRedisService.reserveSlots(saleId, quantity);
        if (result == -1L) {
            // Cache miss: seed from DB then retry once
            slotRedisService.seedIfAbsent(saleId, sale.getTotalSlots() - sale.getReservedSlots());
            result = slotRedisService.reserveSlots(saleId, quantity);
        }
        if (result == 0L) {
            throw new InsufficientSlotsException(saleId);
        }

        // Register a transaction-tied rollback for the Redis deduction. Any subsequent
        // exception that causes this @Transactional method to roll back (write-lock
        // acquisition failure, assertSaleActive throw, save failure, per-user re-check)
        // will fire afterCompletion(ROLLED_BACK) and restore Redis. Without this, a
        // throw between the Redis decrement and the try block on line 192 leaks slots.
        final int rollbackQty = quantity;
        org.springframework.transaction.support.TransactionSynchronizationManager
            .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        slotRedisService.incrementBy(saleId, rollbackQty);
                        log.warn("Tx rolled back — restored {} Redis slots for sale={}", rollbackQty, saleId);
                    }
                }
            });

        // 4. Acquire write lock, re-check window, increment DB counter
        var locked = repository.findByIdWithWriteLock(saleId)
                .orElseThrow(() -> new FlashSaleNotFoundException(saleId));
        assertSaleActive(locked, saleId, clock.instant()); // re-check after lock

        // Re-check per-user quota INSIDE the lock — without this, two concurrent
        // requests from the same user both read alreadyClaimed=0 above and both
        // bypass maxPerUser. Holding the flash_sales row lock serializes all
        // participate() calls for this saleId, so this second sum is authoritative.
        int claimedNow = participationRepo.sumQuantityBySaleIdAndUserId(saleId, userId);
        if (claimedNow + quantity > locked.getMaxPerUser()) {
            throw new UserQuotaExceededException(userId, saleId, locked.getMaxPerUser());
        }

        locked.setReservedSlots(locked.getReservedSlots() + quantity);
        repository.save(locked);

        // 5. Insert participation record
        var participation = new FlashSaleParticipation();
        participation.setSaleId(saleId);
        participation.setUserId(userId);
        participation.setQuantity(quantity);
        FlashSaleParticipation savedParticipation = participationRepo.save(participation);

        // 6. Emit Kafka event AFTER_COMMIT
        int finalRemaining = (int) result;
        eventPublisher.publishEvent(new FlashSaleSlotReservedEvent(
                UUID.randomUUID().toString(), Instant.now(clock),
                saleId, userId, quantity, finalRemaining));

        return new ParticipateResponse(true, savedParticipation.getId(), finalRemaining, null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertSaleActive(FlashSaleJpaEntity sale, String id, Instant now) {
        if (sale.getStatus() != FlashSaleStatus.ACTIVE
                || now.isBefore(sale.getStartTime())
                || now.isAfter(sale.getEndTime())) {
            throw new SaleNotActiveException(id);
        }
    }

    private void transition(FlashSaleJpaEntity entity, FlashSaleStatus target) {
        if (!entity.getStatus().canTransitionTo(target)) {
            throw new InvalidStateTransitionException(entity.getId(), entity.getStatus(), target);
        }
        entity.setStatus(target);
    }

    private FlashSaleJpaEntity findById(String id) {
        return repository.findById(id).orElseThrow(() -> new FlashSaleNotFoundException(id));
    }

    private FlashSaleDTO toDTO(FlashSaleJpaEntity e) {
        return new FlashSaleDTO(
                e.getId(), e.getProductId(), e.getProductName(),
                e.getOriginalPrice(), e.getSalePrice(),
                e.getTotalSlots(), e.getReservedSlots(), e.getMaxPerUser(),
                e.getStartTime(), e.getEndTime(),
                e.getStatus(), e.getDescription(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    private <T> PageDTO<T> toPageDTO(Page<T> page) {
        return new PageDTO<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }
}
