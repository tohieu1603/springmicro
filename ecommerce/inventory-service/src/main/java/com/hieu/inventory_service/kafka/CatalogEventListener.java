package com.hieu.inventory_service.kafka;

import com.hieu.inventory_service.repository.InventoryRepository;
import com.hieu.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes {@code catalog.product-created} and auto-initialises inventory entries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogEventListener {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "catalog.product-created",
        groupId = "inventory-service"
    )
    public void onProductCreated(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode variants = root.path("variants");
            if (!variants.isArray()) {
                log.warn("catalog.product-created: no variants array, skipping key={}", record.key());
                return;
            }
            for (JsonNode v : variants) {
                String variantId = v.path("variantId").asText();
                String sku = v.path("sku").asText();
                int qty = v.path("quantity").asInt(0);
                if (inventoryRepository.existsByProductId(variantId)) {
                    log.debug("Inventory already exists for variantId={}, skipping", variantId);
                    continue;
                }
                try {
                    inventoryService.create(variantId, sku, qty, 10);
                    log.info("Auto-created inventory: variantId={} sku={} qty={}", variantId, sku, qty);
                } catch (Exception ex) {
                    log.warn("Skipping inventory for sku={}: {}", sku, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process catalog.product-created key={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
