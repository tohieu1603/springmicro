package com.hieu.flash_sale_service.service;

import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import com.hieu.flash_sale_service.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * On startup, seeds Redis slot counters for all ACTIVE sales using SET IF NOT EXIST.
 * Survives Redis restarts without double-counting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSyncListener {

    private final FlashSaleRepository repository;
    private final SlotRedisService slotRedisService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void syncRedisCounters() {
        // Stagger multi-pod startup to avoid Redis SETNX stampede
        try {
            Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 3000));
        } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        var activeSales = repository.findAllByStatus(FlashSaleStatus.ACTIVE);
        log.info("Syncing Redis counters for {} ACTIVE sale(s) on startup", activeSales.size());
        for (var sale : activeSales) {
            int remaining = sale.getTotalSlots() - sale.getReservedSlots();
            slotRedisService.seedIfAbsent(sale.getId(), remaining);
            log.debug("Seeded Redis counter saleId={} remaining={}", sale.getId(), remaining);
        }
    }
}
