package com.hieu.inventory_service.service;

import com.hieu.inventory_service.dto.InventoryDTO;
import com.hieu.inventory_service.entity.InventoryEntity;
import com.hieu.inventory_service.entity.StockMovement;
import com.hieu.inventory_service.exception.InventoryNotFoundException;
import com.hieu.inventory_service.kafka.LowStockEventPublisher;
import com.hieu.inventory_service.repository.InventoryRepository;
import com.hieu.inventory_service.repository.StockMovementRepository;
import com.hieu.inventory_service.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the mockable write paths of {@link InventoryService}: create (seeds
 * Redis) and adjustStock (restock / shrink with the reserved-level guard + audit movement).
 * The reserve/confirm/release flows depend on Redis Lua + Spring retry and stay as ITs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService (unit)")
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock StockReservationRepository reservationRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock StockRedisService redisService;
    @Mock LowStockEventPublisher lowStockPublisher;
    @Mock ObjectMapper objectMapper;

    InventoryService service;

    @BeforeEach
    void setup() {
        service = new InventoryService(inventoryRepository, reservationRepository,
                movementRepository, redisService, lowStockPublisher, objectMapper);
    }

    private static InventoryEntity inventory(int quantity, int reserved) {
        return InventoryEntity.builder()
                .id("1").productId("100").sku("SKU-1")
                .quantity(quantity).reservedQuantity(reserved).minStockLevel(5)
                .build();
    }

    @Test
    @DisplayName("create() persists and seeds Redis with the available quantity")
    void create_seedsRedis() {
        when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryDTO dto = service.create("100", "SKU-1", 50, 5);

        assertThat(dto.getProductId()).isEqualTo("100");
        assertThat(dto.getQuantity()).isEqualTo(50);
        verify(redisService).setStock(eq("100"), eq(50));
    }

    @Nested
    @DisplayName("adjustStock()")
    class AdjustStock {

        @Test
        @DisplayName("positive delta restocks and writes an audit movement")
        void positiveDelta() {
            when(inventoryRepository.findAllByProductIdInWithLock(anyList()))
                    .thenReturn(List.of(inventory(50, 10)));
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            InventoryDTO dto = service.adjustStock("100", 20, "admin", "restock");

            assertThat(dto.getQuantity()).isEqualTo(70);
            verify(redisService).invalidate("100");
            verify(movementRepository).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("negative delta shrinks stock when it stays above the reserved level")
        void negativeDeltaOk() {
            when(inventoryRepository.findAllByProductIdInWithLock(anyList()))
                    .thenReturn(List.of(inventory(50, 10)));
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            InventoryDTO dto = service.adjustStock("100", -20, "admin", "shrinkage");

            assertThat(dto.getQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("negative delta that would drop below reserved is rejected")
        void negativeDeltaBelowReserved() {
            when(inventoryRepository.findAllByProductIdInWithLock(anyList()))
                    .thenReturn(List.of(inventory(50, 45)));

            assertThatThrownBy(() -> service.adjustStock("100", -10, "admin", "bad"))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the product has no inventory row")
        void notFound() {
            when(inventoryRepository.findAllByProductIdInWithLock(anyList())).thenReturn(List.of());

            assertThatThrownBy(() -> service.adjustStock("100", 5, "admin", null))
                    .isInstanceOf(InventoryNotFoundException.class);
        }
    }
}
