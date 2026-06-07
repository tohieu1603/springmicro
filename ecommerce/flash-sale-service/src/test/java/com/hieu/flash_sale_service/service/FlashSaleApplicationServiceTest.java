package com.hieu.flash_sale_service.service;

import com.hieu.flash_sale_service.dto.AvailabilityResponse;
import com.hieu.flash_sale_service.dto.CreateFlashSaleRequest;
import com.hieu.flash_sale_service.dto.FlashSaleDTO;
import com.hieu.flash_sale_service.dto.ParticipateResponse;
import com.hieu.flash_sale_service.entity.FlashSaleJpaEntity;
import com.hieu.flash_sale_service.entity.FlashSaleParticipation;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import com.hieu.flash_sale_service.exception.FlashSaleNotFoundException;
import com.hieu.flash_sale_service.exception.InsufficientSlotsException;
import com.hieu.flash_sale_service.exception.InvalidStateTransitionException;
import com.hieu.flash_sale_service.exception.SaleNotActiveException;
import com.hieu.flash_sale_service.exception.UserQuotaExceededException;
import com.hieu.flash_sale_service.kafka.FlashSaleSlotReservedEvent;
import com.hieu.flash_sale_service.kafka.FlashSaleStartedEvent;
import com.hieu.flash_sale_service.repository.FlashSaleParticipationRepository;
import com.hieu.flash_sale_service.repository.FlashSaleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link FlashSaleApplicationService}. A fixed {@link Clock} drives the
 * time-window logic deterministically; repositories, Redis and the event publisher are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlashSaleApplicationService (unit)")
class FlashSaleApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock FlashSaleRepository repository;
    @Mock FlashSaleParticipationRepository participationRepo;
    @Mock SlotRedisService slotRedisService;
    @Mock ApplicationEventPublisher eventPublisher;

    FlashSaleApplicationService service;

    @BeforeEach
    void setup() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new FlashSaleApplicationService(
                repository, participationRepo, slotRedisService, eventPublisher, fixedClock);
        // participate() registers a transaction-tied synchronization; activate one so the
        // call doesn't fail in a non-transactional unit test (rollback cascade is an IT concern).
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private static FlashSaleJpaEntity sale(FlashSaleStatus status, Instant start, Instant end,
                                           int total, int reserved, int maxPerUser) {
        var e = new FlashSaleJpaEntity();
        e.setId("1");
        e.setProductId("p1");
        e.setProductName("Product One");
        e.setOriginalPrice(BigDecimal.valueOf(100));
        e.setSalePrice(BigDecimal.valueOf(80));
        e.setTotalSlots(total);
        e.setReservedSlots(reserved);
        e.setMaxPerUser(maxPerUser);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setStatus(status);
        return e;
    }

    /** A sale that is ACTIVE and currently inside its time window. */
    private static FlashSaleJpaEntity activeSale(int total, int reserved, int maxPerUser) {
        return sale(FlashSaleStatus.ACTIVE,
                NOW.minus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.HOURS),
                total, reserved, maxPerUser);
    }

    @Nested
    @DisplayName("createSale()")
    class CreateSale {

        private CreateFlashSaleRequest request(Instant start, Instant end,
                                               BigDecimal original, BigDecimal sale) {
            return new CreateFlashSaleRequest(
                    "p1", "Product One", original, sale, 10, 2, start, end, "desc");
        }

        @Test
        @DisplayName("persists a SCHEDULED sale with zero reserved slots")
        void create_happyPath() {
            var req = request(NOW.plus(1, ChronoUnit.HOURS), NOW.plus(2, ChronoUnit.HOURS),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(80));
            when(repository.save(any(FlashSaleJpaEntity.class))).thenAnswer(inv -> {
                FlashSaleJpaEntity e = inv.getArgument(0);
                e.setId("42");
                return e;
            });

            FlashSaleDTO dto = service.createSale(req);

            assertThat(dto.status()).isEqualTo(FlashSaleStatus.SCHEDULED);
            assertThat(dto.reservedSlots()).isZero();
            assertThat(dto.totalSlots()).isEqualTo(10);
            assertThat(dto.salePrice()).isEqualByComparingTo(BigDecimal.valueOf(80));
            verify(repository).save(any(FlashSaleJpaEntity.class));
        }

        @Test
        @DisplayName("rejects startTime not before endTime")
        void create_startAfterEnd() {
            var req = request(NOW.plus(3, ChronoUnit.HOURS), NOW.plus(2, ChronoUnit.HOURS),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(80));
            assertThatThrownBy(() -> service.createSale(req))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a duration shorter than the 5-minute minimum")
        void create_tooShort() {
            var req = request(NOW.plus(1, ChronoUnit.HOURS),
                    NOW.plus(1, ChronoUnit.HOURS).plus(2, ChronoUnit.MINUTES),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(80));
            assertThatThrownBy(() -> service.createSale(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a startTime in the past")
        void create_startInPast() {
            var req = request(NOW.minus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.HOURS),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(80));
            assertThatThrownBy(() -> service.createSale(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects salePrice >= originalPrice")
        void create_salePriceNotLower() {
            var req = request(NOW.plus(1, ChronoUnit.HOURS), NOW.plus(2, ChronoUnit.HOURS),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(100));
            assertThatThrownBy(() -> service.createSale(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("checkAvailability()")
    class CheckAvailability {

        @Test
        @DisplayName("uses the Redis counter when present")
        void availability_redisValue() {
            when(repository.findById("1")).thenReturn(Optional.of(activeSale(10, 3, 5)));
            when(slotRedisService.getRemaining("1")).thenReturn(7);

            AvailabilityResponse resp = service.checkAvailability("1");

            assertThat(resp.available()).isTrue();
            assertThat(resp.remainingSlots()).isEqualTo(7);
            assertThat(resp.reason()).isNull();
        }

        @Test
        @DisplayName("falls back to total-reserved when Redis is empty")
        void availability_dbFallback() {
            when(repository.findById("1")).thenReturn(Optional.of(activeSale(10, 4, 5)));
            when(slotRedisService.getRemaining("1")).thenReturn(null);

            AvailabilityResponse resp = service.checkAvailability("1");

            assertThat(resp.remainingSlots()).isEqualTo(6);
            assertThat(resp.available()).isTrue();
        }

        @Test
        @DisplayName("is unavailable with a reason when the sale is not ACTIVE")
        void availability_notActive() {
            var scheduled = sale(FlashSaleStatus.SCHEDULED,
                    NOW.minus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.HOURS), 10, 0, 5);
            when(repository.findById("1")).thenReturn(Optional.of(scheduled));
            when(slotRedisService.getRemaining("1")).thenReturn(10);

            AvailabilityResponse resp = service.checkAvailability("1");

            assertThat(resp.available()).isFalse();
            assertThat(resp.reason()).contains("SCHEDULED");
        }

        @Test
        @DisplayName("throws when the sale does not exist")
        void availability_notFound() {
            when(repository.findById("99")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.checkAvailability("99"))
                    .isInstanceOf(FlashSaleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("activateSale()")
    class ActivateSale {

        @Test
        @DisplayName("transitions SCHEDULED→ACTIVE, seeds Redis and emits a started event")
        void activate_happyPath() {
            var scheduled = sale(FlashSaleStatus.SCHEDULED,
                    NOW.plus(1, ChronoUnit.HOURS), NOW.plus(2, ChronoUnit.HOURS), 10, 0, 5);
            when(repository.findById("1")).thenReturn(Optional.of(scheduled));
            when(repository.save(any(FlashSaleJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            FlashSaleDTO dto = service.activateSale("1");

            assertThat(dto.status()).isEqualTo(FlashSaleStatus.ACTIVE);
            verify(slotRedisService).seed(eq("1"), eq(10));
            verify(eventPublisher).publishEvent(any(FlashSaleStartedEvent.class));
        }

        @Test
        @DisplayName("rejects activating a terminal (ENDED) sale")
        void activate_illegalTransition() {
            var ended = sale(FlashSaleStatus.ENDED,
                    NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), 10, 10, 5);
            when(repository.findById("1")).thenReturn(Optional.of(ended));

            assertThatThrownBy(() -> service.activateSale("1"))
                    .isInstanceOf(InvalidStateTransitionException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("participate()")
    class Participate {

        @Test
        @DisplayName("reserves slots, records participation and emits a reserved event")
        void participate_happyPath() {
            when(repository.findByIdWithReadLock("1")).thenReturn(Optional.of(activeSale(10, 0, 5)));
            when(repository.findByIdWithWriteLock("1")).thenReturn(Optional.of(activeSale(10, 0, 5)));
            when(participationRepo.sumQuantityBySaleIdAndUserId("1", "u1")).thenReturn(0);
            when(slotRedisService.reserveSlots("1", 2)).thenReturn(8L);
            var saved = new FlashSaleParticipation();
            saved.setId("100");
            when(participationRepo.save(any(FlashSaleParticipation.class))).thenReturn(saved);

            ParticipateResponse resp = service.participate("1", "u1", 2);

            assertThat(resp.success()).isTrue();
            assertThat(resp.participationId()).isEqualTo("100");
            assertThat(resp.remainingSlots()).isEqualTo(8);
            verify(eventPublisher).publishEvent(any(FlashSaleSlotReservedEvent.class));
        }

        @Test
        @DisplayName("on a Redis cache miss, seeds from DB then retries the reservation")
        void participate_cacheMissThenSeed() {
            when(repository.findByIdWithReadLock("1")).thenReturn(Optional.of(activeSale(10, 0, 5)));
            when(repository.findByIdWithWriteLock("1")).thenReturn(Optional.of(activeSale(10, 0, 5)));
            when(participationRepo.sumQuantityBySaleIdAndUserId("1", "u1")).thenReturn(0);
            when(slotRedisService.reserveSlots("1", 2)).thenReturn(-1L, 5L); // miss, then success
            var saved = new FlashSaleParticipation();
            saved.setId("101");
            when(participationRepo.save(any(FlashSaleParticipation.class))).thenReturn(saved);

            ParticipateResponse resp = service.participate("1", "u1", 2);

            assertThat(resp.remainingSlots()).isEqualTo(5);
            verify(slotRedisService).seedIfAbsent(eq("1"), eq(10));
        }

        @Test
        @DisplayName("rejects when the per-user quota would be exceeded")
        void participate_quotaExceeded() {
            when(repository.findByIdWithReadLock("1")).thenReturn(Optional.of(activeSale(10, 0, 2)));
            when(participationRepo.sumQuantityBySaleIdAndUserId("1", "u1")).thenReturn(2);

            assertThatThrownBy(() -> service.participate("1", "u1", 2))
                    .isInstanceOf(UserQuotaExceededException.class);
            verify(slotRedisService, never()).reserveSlots(anyString(), anyInt());
        }

        @Test
        @DisplayName("rejects when there are insufficient slots")
        void participate_insufficientSlots() {
            when(repository.findByIdWithReadLock("1")).thenReturn(Optional.of(activeSale(10, 0, 5)));
            when(participationRepo.sumQuantityBySaleIdAndUserId("1", "u1")).thenReturn(0);
            when(slotRedisService.reserveSlots("1", 2)).thenReturn(0L);

            assertThatThrownBy(() -> service.participate("1", "u1", 2))
                    .isInstanceOf(InsufficientSlotsException.class);
        }

        @Test
        @DisplayName("rejects when the sale is not active")
        void participate_notActive() {
            var scheduled = sale(FlashSaleStatus.SCHEDULED,
                    NOW.plus(1, ChronoUnit.HOURS), NOW.plus(2, ChronoUnit.HOURS), 10, 0, 5);
            when(repository.findByIdWithReadLock("1")).thenReturn(Optional.of(scheduled));

            assertThatThrownBy(() -> service.participate("1", "u1", 2))
                    .isInstanceOf(SaleNotActiveException.class);
        }
    }
}
