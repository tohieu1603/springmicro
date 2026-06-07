package com.hieu.flash_sale_service.service;

import com.hieu.flash_sale_service.AbstractIntegrationTest;
import com.hieu.flash_sale_service.entity.FlashSaleJpaEntity;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import com.hieu.flash_sale_service.repository.FlashSaleParticipationRepository;
import com.hieu.flash_sale_service.repository.FlashSaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StartupSyncListener — IT")
class StartupSyncListenerIT extends AbstractIntegrationTest {

    @Autowired StartupSyncListener          syncListener;
    @Autowired FlashSaleRepository          saleRepo;
    @Autowired FlashSaleParticipationRepository participationRepo;
    @Autowired SlotRedisService             slotRedisService;
    @Autowired StringRedisTemplate          redisTemplate;

    @BeforeEach
    void cleanUp() {
        participationRepo.deleteAll();
        saleRepo.deleteAll();
        var keys = redisTemplate.keys("flashsale:slots:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    FlashSaleJpaEntity createActiveSale(int totalSlots, int reservedSlots) {
        var entity = new FlashSaleJpaEntity();
        entity.setProductId("prod-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setProductName("Sync Test Sale");
        entity.setOriginalPrice(BigDecimal.valueOf(1_000_000));
        entity.setSalePrice(BigDecimal.valueOf(700_000));
        entity.setTotalSlots(totalSlots);
        entity.setReservedSlots(reservedSlots);
        entity.setMaxPerUser(5);
        entity.setStartTime(Instant.now().minusSeconds(3600));
        entity.setEndTime(Instant.now().plusSeconds(3600));
        entity.setStatus(FlashSaleStatus.ACTIVE);
        return saleRepo.save(entity);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("seedRedisFromDb")
    class SeedRedisFromDb {

        @Test
        @DisplayName("idempotent: calling syncRedisCounters twice does not overwrite existing key")
        void seedRedisFromDb_idempotent() throws InterruptedException {
            var sale = createActiveSale(100, 20);
            // No Redis key yet — first call should seed it
            long before = System.currentTimeMillis();
            syncListener.syncRedisCounters();
            long after = System.currentTimeMillis();

            Integer afterFirst = slotRedisService.getRemaining(sale.getId());
            assertThat(afterFirst)
                    .as("after first sync, Redis should have remaining=80 (100-20)")
                    .isEqualTo(80);

            // Simulate external decrement (user reserved 5 more while service is running)
            slotRedisService.seed(sale.getId(), 75);

            // Second call must NOT overwrite the existing key (seedIfAbsent semantics)
            syncListener.syncRedisCounters();

            Integer afterSecond = slotRedisService.getRemaining(sale.getId());
            assertThat(afterSecond)
                    .as("second sync must not overwrite Redis — key already exists (idempotent)")
                    .isEqualTo(75);

            // Verify jitter: the listener sleeps 0-3s, so total wall time should be reasonable
            // (just validate that both calls completed within 10 seconds total)
            assertThat(System.currentTimeMillis() - before)
                    .as("both sync calls should complete within 10 seconds (jitter≤3s each)")
                    .isLessThan(10_000L);
        }

        @Test
        @DisplayName("SCHEDULED sale is skipped — only ACTIVE sales are seeded")
        void seedRedisFromDb_scheduledSaleSkipped() throws InterruptedException {
            var activeSale = createActiveSale(50, 10);

            var scheduledEntity = new FlashSaleJpaEntity();
            scheduledEntity.setProductId("prod-sched-" + UUID.randomUUID().toString().substring(0, 8));
            scheduledEntity.setOriginalPrice(BigDecimal.valueOf(500_000));
            scheduledEntity.setSalePrice(BigDecimal.valueOf(400_000));
            scheduledEntity.setTotalSlots(30);
            scheduledEntity.setReservedSlots(0);
            scheduledEntity.setMaxPerUser(2);
            scheduledEntity.setStartTime(Instant.now().plusSeconds(7200));
            scheduledEntity.setEndTime(Instant.now().plusSeconds(14400));
            scheduledEntity.setStatus(FlashSaleStatus.SCHEDULED);
            var scheduled = saleRepo.save(scheduledEntity);

            syncListener.syncRedisCounters();

            // Active sale should be seeded
            assertThat(slotRedisService.getRemaining(activeSale.getId()))
                    .as("ACTIVE sale must be seeded in Redis")
                    .isEqualTo(40); // 50 - 10

            // Scheduled sale must NOT be seeded
            assertThat(slotRedisService.getRemaining(scheduled.getId()))
                    .as("SCHEDULED sale must NOT be seeded in Redis")
                    .isNull();
        }
    }
}
