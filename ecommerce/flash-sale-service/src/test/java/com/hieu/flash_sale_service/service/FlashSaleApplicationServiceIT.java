package com.hieu.flash_sale_service.service;

import com.hieu.flash_sale_service.AbstractIntegrationTest;
import com.hieu.flash_sale_service.dto.CreateFlashSaleRequest;
import com.hieu.flash_sale_service.entity.FlashSaleJpaEntity;
import com.hieu.flash_sale_service.entity.FlashSaleParticipation;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import com.hieu.flash_sale_service.exception.InsufficientSlotsException;
import com.hieu.flash_sale_service.repository.FlashSaleParticipationRepository;
import com.hieu.flash_sale_service.repository.FlashSaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DisplayName("FlashSaleApplicationService — IT")
class FlashSaleApplicationServiceIT extends AbstractIntegrationTest {

    @Autowired FlashSaleApplicationService saleService;
    @Autowired FlashSaleRepository          saleRepo;
    @Autowired FlashSaleParticipationRepository participationRepo;
    @Autowired SlotRedisService             slotRedisService;
    @Autowired StringRedisTemplate          redisTemplate;

    @BeforeEach
    void cleanUp() {
        participationRepo.deleteAll();
        saleRepo.deleteAll();
        // Clear Redis keys
        var keys = redisTemplate.keys("flashsale:slots:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    /**
     * Creates a sale directly via repository (bypasses startTime-in-future validation),
     * sets status=ACTIVE and time window to [now-1h, now+1h] for immediate participation.
     */
    FlashSaleJpaEntity createActiveSale(int totalSlots, int maxPerUser) {
        var entity = new FlashSaleJpaEntity();
        entity.setProductId("prod-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setProductName("Test Product");
        entity.setOriginalPrice(BigDecimal.valueOf(1_000_000));
        entity.setSalePrice(BigDecimal.valueOf(800_000));
        entity.setTotalSlots(totalSlots);
        entity.setReservedSlots(0);
        entity.setMaxPerUser(maxPerUser);
        entity.setStartTime(Instant.now().minusSeconds(3600));
        entity.setEndTime(Instant.now().plusSeconds(3600));
        entity.setStatus(FlashSaleStatus.ACTIVE);

        var saved = saleRepo.save(entity);
        // Seed Redis counter to match DB
        slotRedisService.seed(saved.getId(), totalSlots);
        return saved;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("participate — happy path")
    class Participate {

        @Test
        @DisplayName("participate qty=5 → Redis decremented by 5, DB reservedSlots=5, participation row exists")
        void participate_decrementsRedisAndDb_atomically() {
            var sale = createActiveSale(100, 10);
            String userId = UUID.randomUUID().toString();

            saleService.participate(sale.getId(), userId, 5);

            // DB check
            var updated = saleRepo.findById(sale.getId()).orElseThrow();
            assertThat(updated.getReservedSlots())
                    .as("reservedSlots in DB should be 5")
                    .isEqualTo(5);

            // Participation record check
            int claimed = participationRepo.sumQuantityBySaleIdAndUserId(sale.getId(), userId);
            assertThat(claimed)
                    .as("participation record quantity should be 5")
                    .isEqualTo(5);

            // Redis check
            Integer redisRemaining = slotRedisService.getRemaining(sale.getId());
            assertThat(redisRemaining)
                    .as("Redis slot counter should be 95 after reserving 5 out of 100")
                    .isEqualTo(95);
        }
    }

    @Nested
    @DisplayName("participate — over-subscribe")
    class OverSubscribe {

        @Test
        @DisplayName("slots=10 used=8 via Redis, request qty=5 → InsufficientSlotsException")
        void participate_overSubscribe_throwsInsufficient() {
            var sale = createActiveSale(10, 10);
            // Pre-consume 8 slots via Redis directly so only 2 remain
            slotRedisService.seed(sale.getId(), 2);   // override to 2 remaining

            // Also update DB to reflect used=8 (pre-check passes, but Redis will fail)
            var entity = saleRepo.findById(sale.getId()).orElseThrow();
            entity.setReservedSlots(8);
            saleRepo.save(entity);

            String userId = UUID.randomUUID().toString();
            assertThatThrownBy(() -> saleService.participate(sale.getId(), userId, 5))
                    .isInstanceOf(InsufficientSlotsException.class)
                    .hasMessageContaining(sale.getId().toString());
        }
    }

    @Nested
    @DisplayName("participate — partial failure rollback")
    class PartialFailureRollback {

        // MockitoBean scoped to this test class context — replaces participationRepo bean
        @MockitoBean
        FlashSaleParticipationRepository mockParticipationRepo;

        @Test
        @DisplayName("participationRepo.save throws → Redis slots restored to original value")
        void participate_participationSaveFails_rollsBackRedis() {
            // Cannot use createActiveSale() here because mockParticipationRepo replaces the real one
            // Use real saleRepo which is still injected
            var entity = new FlashSaleJpaEntity();
            entity.setProductId("prod-rollback");
            entity.setProductName("Rollback Product");
            entity.setOriginalPrice(BigDecimal.valueOf(500_000));
            entity.setSalePrice(BigDecimal.valueOf(300_000));
            entity.setTotalSlots(50);
            entity.setReservedSlots(0);
            entity.setMaxPerUser(10);
            entity.setStartTime(Instant.now().minusSeconds(3600));
            entity.setEndTime(Instant.now().plusSeconds(3600));
            entity.setStatus(FlashSaleStatus.ACTIVE);
            var saved = saleRepo.save(entity);
            slotRedisService.seed(saved.getId(), 50);

            // Make sumQuantityBySaleIdAndUserId return 0 (no prior participation)
            org.mockito.Mockito.when(mockParticipationRepo
                    .sumQuantityBySaleIdAndUserId(any(), any())).thenReturn(0);
            // Make save throw
            doThrow(new RuntimeException("DB write failed"))
                    .when(mockParticipationRepo).save(any(FlashSaleParticipation.class));

            String userId = UUID.randomUUID().toString();
            assertThatThrownBy(() -> saleService.participate(saved.getId(), userId, 3))
                    .isInstanceOf(RuntimeException.class);

            // Redis should be back to 50 after rollback
            Integer remaining = slotRedisService.getRemaining(saved.getId());
            assertThat(remaining)
                    .as("Redis slots must be restored after rollback")
                    .isEqualTo(50);
        }
    }
}
