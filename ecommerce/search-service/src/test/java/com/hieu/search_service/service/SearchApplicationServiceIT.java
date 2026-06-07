package com.hieu.search_service.service;

import com.hieu.search_service.AbstractIntegrationTest;
import com.hieu.search_service.document.ProductDocument;
import com.hieu.search_service.dto.IndexProductRequest;
import com.hieu.search_service.dto.PageResponse;
import com.hieu.search_service.dto.SearchRequest;
import com.hieu.search_service.repository.ProductSearchRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchApplicationService — Integration")
class SearchApplicationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private SearchApplicationService service;

    @Autowired
    private ProductSearchRepository repository;

    @BeforeEach
    void cleanIndex() {
        repository.deleteAll();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private IndexProductRequest buildRequest(String id, String name, Double price) {
        return IndexProductRequest.builder()
                .id(id)
                .name(name)
                .description("desc-" + id)
                .sku("SKU-" + id)
                .categoryId("cat-1")
                .categoryName("Electronics")
                .brand("TestBrand")
                .price(price)
                .minPrice(price)
                .maxPrice(price)
                .totalStock(10)
                .status("ACTIVE")
                .tags(List.of("tag1"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ── test groups ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Index and Search")
    class IndexAndSearch {

        @Test
        @DisplayName("indexProduct then searchByName returns 1 hit")
        void indexProduct_then_searchByName_returnsHit() {
            service.indexProduct(buildRequest("prod-1", "Laptop Pro 15", 1200.0));

            SearchRequest req = SearchRequest.builder().q("laptop").page(0).size(10).build();

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                PageResponse<ProductDocument> result = service.searchProducts(req);
                assertThat(result.getContent())
                        .hasSize(1)
                        .first()
                        .extracting(ProductDocument::getName)
                        .isEqualTo("Laptop Pro 15");
            });
        }

        @Test
        @DisplayName("deleteProduct removes document from index")
        void deleteProduct_removesFromIndex() {
            service.indexProduct(buildRequest("prod-del", "DeleteMe TV", 300.0));

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(repository.findById("prod-del")).isPresent());

            service.removeProduct("prod-del");

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(repository.findById("prod-del")).isEmpty());
        }
    }

    @Nested
    @DisplayName("Sort-field allowlist")
    class SortAllowlist {

        @Test
        @DisplayName("sortBy with invalid/malicious field falls back to createdAt — no error")
        void searchWithSortBy_invalidField_fallsBackToCreatedAt() {
            service.indexProduct(buildRequest("prod-s1", "Sort Test Phone", 500.0));

            // sortBy with path-traversal attempt must not throw and should use default sort
            SearchRequest req = SearchRequest.builder()
                    .q("Sort Test")
                    .sortBy("../malicious")
                    .page(0).size(10)
                    .build();

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                PageResponse<ProductDocument> result = service.searchProducts(req);
                // Result returns without exception; allowlist enforced
                assertThat(result).isNotNull();
                assertThat(result.getContent()).isNotEmpty();
            });
        }

        @Test
        @DisplayName("sortBy valid field 'price' works and returns results")
        void searchWithSortBy_validField_works() {
            service.indexProduct(buildRequest("prod-p1", "Sort Watch Alpha", 200.0));
            service.indexProduct(buildRequest("prod-p2", "Sort Watch Beta", 100.0));
            service.indexProduct(buildRequest("prod-p3", "Sort Watch Gamma", 300.0));

            SearchRequest req = SearchRequest.builder()
                    .q("Sort Watch")
                    .sortBy("price")
                    .page(0).size(10)
                    .build();

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                PageResponse<ProductDocument> result = service.searchProducts(req);
                assertThat(result.getContent()).hasSize(3);
                // sorted desc by price: 300, 200, 100
                List<Double> prices = result.getContent().stream()
                        .map(ProductDocument::getPrice)
                        .toList();
                assertThat(prices).containsExactly(300.0, 200.0, 100.0);
            });
        }
    }
}
