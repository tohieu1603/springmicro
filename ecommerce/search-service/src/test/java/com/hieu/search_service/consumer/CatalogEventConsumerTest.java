package com.hieu.search_service.consumer;

import com.hieu.search_service.dto.IndexProductRequest;
import com.hieu.search_service.service.SearchApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pure unit tests for {@link CatalogEventConsumer}'s payload→{@link IndexProductRequest}
 * mapping and event routing. {@link SearchApplicationService} is mocked; we invoke the
 * public {@code @KafkaListener} methods directly with representative {@code Map} payloads
 * (Kafka is never started) and assert the mapping via {@link ArgumentCaptor}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogEventConsumer (unit)")
class CatalogEventConsumerTest {

    @Mock SearchApplicationService searchService;

    @InjectMocks CatalogEventConsumer consumer;

    @Captor ArgumentCaptor<IndexProductRequest> reqCaptor;

    /** A representative "full" created/updated payload with a variants list. */
    private static Map<String, Object> fullPayload() {
        Map<String, Object> p = new HashMap<>();
        p.put("productId", 42L);
        p.put("name", "Laptop");
        p.put("description", "A nice laptop");
        p.put("sku", "SKU-42");
        p.put("categoryId", "cat-1");
        p.put("categoryName", "Electronics");
        p.put("brand", "Acme");
        p.put("price", 1000.0);
        p.put("status", "ACTIVE");
        p.put("thumbnail", "http://img/thumb.png");
        p.put("tags", List.of("portable", "work"));
        p.put("variants", List.of(
                Map.of("price", 1200.0, "quantity", 3),
                Map.of("price", 900.0, "quantity", 7),
                Map.of("price", 1100.0, "quantity", 5)
        ));
        return p;
    }

    @Nested
    @DisplayName("onProductCreated() / onProductUpdated() — mapping")
    class IndexMapping {

        @Test
        @DisplayName("aggregates variants[] into min/max price and total stock")
        void aggregatesVariants() {
            consumer.onProductCreated(fullPayload());

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getMinPrice()).isEqualTo(900.0);
            assertThat(req.getMaxPrice()).isEqualTo(1200.0);
            assertThat(req.getTotalStock()).isEqualTo(15); // 3 + 7 + 5
        }

        @Test
        @DisplayName("id falls back to 'productId' when 'id' absent (Long → String)")
        void idFallsBackToProductId() {
            consumer.onProductCreated(fullPayload());

            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getId()).isEqualTo("42");
        }

        @Test
        @DisplayName("'id' takes precedence over 'productId' when both present")
        void idPrefersIdOverProductId() {
            Map<String, Object> p = fullPayload();
            p.put("id", "explicit-id");

            consumer.onProductUpdated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getId()).isEqualTo("explicit-id");
        }

        @Test
        @DisplayName("imageUrl uses 'thumbnail' first, then falls back to 'imageUrl'")
        void thumbnailFallback() {
            // thumbnail present -> wins
            consumer.onProductCreated(fullPayload());
            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getImageUrl()).isEqualTo("http://img/thumb.png");
        }

        @Test
        @DisplayName("imageUrl falls back to 'imageUrl' key when 'thumbnail' absent")
        void imageUrlFallbackWhenNoThumbnail() {
            Map<String, Object> p = fullPayload();
            p.remove("thumbnail");
            p.put("imageUrl", "http://img/fallback.png");

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getImageUrl()).isEqualTo("http://img/fallback.png");
        }

        @Test
        @DisplayName("maps the tags list, stringifying each element")
        void mapsTags() {
            consumer.onProductUpdated(fullPayload());

            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getTags()).containsExactly("portable", "work");
        }

        @Test
        @DisplayName("tags default to empty list when payload has no tags")
        void tagsDefaultEmpty() {
            Map<String, Object> p = fullPayload();
            p.remove("tags");

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getTags()).isEmpty();
        }

        @Test
        @DisplayName("maps the simple scalar fields verbatim")
        void mapsScalarFields() {
            consumer.onProductCreated(fullPayload());

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getName()).isEqualTo("Laptop");
            assertThat(req.getDescription()).isEqualTo("A nice laptop");
            assertThat(req.getSku()).isEqualTo("SKU-42");
            assertThat(req.getCategoryId()).isEqualTo("cat-1");
            assertThat(req.getCategoryName()).isEqualTo("Electronics");
            assertThat(req.getBrand()).isEqualTo("Acme");
            assertThat(req.getPrice()).isEqualTo(1000.0);
            assertThat(req.getStatus()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("numeric coercion (string vs number)")
    class NumericCoercion {

        @Test
        @DisplayName("coerces numeric String prices/quantities in variants")
        void coercesStringNumericsInVariants() {
            Map<String, Object> p = new HashMap<>();
            p.put("productId", "p-str");
            p.put("variants", List.of(
                    Map.of("price", "150.5", "quantity", "2"),
                    Map.of("price", "99.5", "quantity", "8")
            ));

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getMinPrice()).isEqualTo(99.5);
            assertThat(req.getMaxPrice()).isEqualTo(150.5);
            assertThat(req.getTotalStock()).isEqualTo(10);
        }

        @Test
        @DisplayName("coerces top-level numeric String price/totalStock when no variants")
        void coercesTopLevelStringNumerics() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-top");
            p.put("price", "49.99");
            p.put("totalStock", "12");

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getPrice()).isEqualTo(49.99);
            assertThat(req.getTotalStock()).isEqualTo(12);
        }

        @Test
        @DisplayName("non-numeric String price/stock coerce to null (not an exception)")
        void nonNumericCoercesToNull() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-bad");
            p.put("price", "not-a-number");
            p.put("totalStock", "NaN-ish");

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getPrice()).isNull();
            assertThat(req.getTotalStock()).isNull();
        }
    }

    @Nested
    @DisplayName("variant aggregation edge cases / fallbacks")
    class VariantEdgeCases {

        @Test
        @DisplayName("no variants → min/max/totalStock fall back to top-level keys")
        void noVariantsFallsBackToTopLevel() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-novar");
            p.put("minPrice", 10.0);
            p.put("maxPrice", 20.0);
            p.put("totalStock", 5);

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getMinPrice()).isEqualTo(10.0);
            assertThat(req.getMaxPrice()).isEqualTo(20.0);
            assertThat(req.getTotalStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("empty variants list → aggregates empty, top-level fallbacks apply")
        void emptyVariantsList() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-empty");
            p.put("variants", List.of());
            p.put("minPrice", 7.0);

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getMinPrice()).isEqualTo(7.0);
            assertThat(req.getMaxPrice()).isNull();
        }

        @Test
        @DisplayName("variants present override top-level min/max/stock")
        void variantsOverrideTopLevel() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-override");
            p.put("minPrice", 1.0);   // should be ignored — variants win
            p.put("maxPrice", 2.0);
            p.put("totalStock", 99);
            p.put("variants", List.of(
                    Map.of("price", 50.0, "quantity", 4),
                    Map.of("price", 80.0, "quantity", 6)
            ));

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getMinPrice()).isEqualTo(50.0);
            assertThat(req.getMaxPrice()).isEqualTo(80.0);
            assertThat(req.getTotalStock()).isEqualTo(10);
        }

        @Test
        @DisplayName("variants with only quantities → stock summed, price aggregates null")
        void variantsOnlyQuantities() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-qtyonly");
            p.put("variants", List.of(
                    Map.of("quantity", 2),
                    Map.of("quantity", 3)
            ));

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getTotalStock()).isEqualTo(5);
            assertThat(req.getMinPrice()).isNull();
            assertThat(req.getMaxPrice()).isNull();
        }

        @Test
        @DisplayName("non-Map entries in variants list are skipped")
        void nonMapVariantsSkipped() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-mixed");
            List<Object> vars = List.of(
                    "garbage",
                    Map.of("price", 30.0, "quantity", 4)
            );
            p.put("variants", vars);

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getMinPrice()).isEqualTo(30.0);
            assertThat(req.getMaxPrice()).isEqualTo(30.0);
            assertThat(req.getTotalStock()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("timestamp parsing")
    class Timestamps {

        @Test
        @DisplayName("parses ISO-8601 String and epoch-millis Number timestamps")
        void parsesTimestamps() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-ts");
            p.put("createdAt", "2024-01-02T03:04:05Z");
            p.put("updatedAt", 1_700_000_000_000L);

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            IndexProductRequest req = reqCaptor.getValue();
            assertThat(req.getCreatedAt()).isEqualTo(Instant.parse("2024-01-02T03:04:05Z"));
            assertThat(req.getUpdatedAt()).isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
        }

        @Test
        @DisplayName("unparseable timestamp coerces to null")
        void unparseableTimestampNull() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-badts");
            p.put("createdAt", "yesterday");

            consumer.onProductCreated(p);

            verify(searchService).indexProduct(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getCreatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("onProductStatusChanged() — partial update routing")
    class StatusChanged {

        @Test
        @DisplayName("routes to updateStatus(id, newStatus); never calls indexProduct")
        void routesToUpdateStatus() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-1");
            p.put("newStatus", "INACTIVE");

            consumer.onProductStatusChanged(p);

            verify(searchService).updateStatus("p-1", "INACTIVE");
            verify(searchService, never()).indexProduct(any());
        }

        @Test
        @DisplayName("id falls back to 'productId' when 'id' absent")
        void statusIdFallsBackToProductId() {
            Map<String, Object> p = new HashMap<>();
            p.put("productId", 7L);
            p.put("newStatus", "ARCHIVED");

            consumer.onProductStatusChanged(p);

            verify(searchService).updateStatus("7", "ARCHIVED");
        }

        @Test
        @DisplayName("missing id → no service interaction")
        void missingIdNoOp() {
            Map<String, Object> p = new HashMap<>();
            p.put("newStatus", "INACTIVE");

            consumer.onProductStatusChanged(p);

            verifyNoInteractions(searchService);
        }

        @Test
        @DisplayName("missing newStatus → no service interaction")
        void missingNewStatusNoOp() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-1");

            consumer.onProductStatusChanged(p);

            verifyNoInteractions(searchService);
        }
    }

    @Nested
    @DisplayName("onProductDeleted() — remove routing")
    class Deleted {

        @Test
        @DisplayName("routes to removeProduct(id)")
        void routesToRemove() {
            consumer.onProductDeleted(Map.of("id", "del-1"));

            verify(searchService).removeProduct("del-1");
            verify(searchService, never()).indexProduct(any());
        }

        @Test
        @DisplayName("missing 'id' (does NOT fall back to productId) → no-op")
        void deletedMissingIdNoOp() {
            // onProductDeleted only reads "id", never "productId"
            consumer.onProductDeleted(Map.of("productId", 99L));

            verify(searchService, never()).removeProduct(any());
            verifyNoInteractions(searchService);
        }
    }

    @Nested
    @DisplayName("event routing isolation")
    class RoutingIsolation {

        @Test
        @DisplayName("created/updated route to indexProduct, not updateStatus/remove")
        void createdRoutesOnlyToIndex() {
            consumer.onProductCreated(fullPayload());

            verify(searchService).indexProduct(any(IndexProductRequest.class));
            verify(searchService, never()).updateStatus(any(), any());
            verify(searchService, never()).removeProduct(any());
        }

        @Test
        @DisplayName("status-changed does not remove or index")
        void statusChangedRoutingIsolation() {
            Map<String, Object> p = new HashMap<>();
            p.put("id", "p-1");
            p.put("newStatus", "ACTIVE");

            consumer.onProductStatusChanged(p);

            verify(searchService).updateStatus(eq("p-1"), eq("ACTIVE"));
            verify(searchService, never()).removeProduct(any());
            verify(searchService, never()).indexProduct(any());
        }
    }
}
