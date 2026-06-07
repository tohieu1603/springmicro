package com.hieu.search_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hieu.search_service.AbstractIntegrationTest;
import com.hieu.search_service.repository.ProductSearchRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogEventConsumer — Integration")
class CatalogEventConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProductSearchRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanIndex() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("catalog.product-created")
    class ProductCreated {

        @Test
        @DisplayName("catalog.product-created event indexes document into Elasticsearch")
        void catalogProductCreated_indexesIntoEs() throws Exception {
            Map<String, Object> payload = Map.of(
                    "productId", "evt-prod-1",
                    "name", "Event Laptop",
                    "description", "From event",
                    "sku", "SKU-EVT-1",
                    "categoryId", "cat-evt",
                    "categoryName", "Electronics",
                    "brand", "EventBrand",
                    "price", 999.0,
                    "totalStock", 5,
                    "status", "ACTIVE"
            );
            kafkaTemplate.send("catalog.product-created", objectMapper.writeValueAsString(payload));

            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                            assertThat(repository.findById("evt-prod-1")).isPresent());
        }
    }

    @Nested
    @DisplayName("catalog.product-deleted")
    class ProductDeleted {

        @Test
        @DisplayName("catalog.product-deleted event removes document from Elasticsearch")
        void catalogProductDeleted_removesDoc() throws Exception {
            // Pre-index document directly
            Map<String, Object> createPayload = Map.of(
                    "productId", "evt-prod-del",
                    "name", "To Be Deleted",
                    "description", "Will be deleted",
                    "sku", "SKU-DEL",
                    "categoryId", "cat-1",
                    "categoryName", "Electronics",
                    "brand", "Brand",
                    "price", 100.0,
                    "totalStock", 1,
                    "status", "ACTIVE"
            );
            kafkaTemplate.send("catalog.product-created", objectMapper.writeValueAsString(createPayload));

            // Wait for doc to appear
            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                            assertThat(repository.findById("evt-prod-del")).isPresent());

            // Publish delete event
            Map<String, Object> deletePayload = Map.of("id", "evt-prod-del");
            kafkaTemplate.send("catalog.product-deleted", objectMapper.writeValueAsString(deletePayload));

            // Wait for doc removal (removeProduct is async)
            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                            assertThat(repository.findById("evt-prod-del")).isEmpty());
        }
    }
}
