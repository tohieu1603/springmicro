package com.hieu.search_service.service;

import com.hieu.search_service.document.ProductDocument;
import com.hieu.search_service.dto.IndexProductRequest;
import com.hieu.search_service.repository.ProductSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the mockable write paths of {@link SearchApplicationService}:
 * request→document mapping (index/reindex) and the partial status update. Query paths
 * ({@code searchProducts}/{@code getSuggestions}) need a real Elasticsearch and are
 * covered by the integration tests instead.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchApplicationService (unit)")
class SearchApplicationServiceTest {

    @Mock ProductSearchRepository repository;
    @Mock ElasticsearchOperations operations;

    @InjectMocks SearchApplicationService service;

    @Captor ArgumentCaptor<ProductDocument> docCaptor;

    private static IndexProductRequest request(String id, String name) {
        return IndexProductRequest.builder()
                .id(id)
                .name(name)
                .description("desc")
                .sku("SKU-" + id)
                .categoryId("c1")
                .categoryName("Category")
                .brand("BrandX")
                .price(199.0)
                .minPrice(149.0)
                .maxPrice(249.0)
                .totalStock(20)
                .status("ACTIVE")
                .imageUrl("http://img/" + id)
                .tags(List.of("a", "b"))
                .build();
    }

    @Nested
    @DisplayName("indexProduct()")
    class IndexProduct {

        @Test
        @DisplayName("maps every request field onto the saved document")
        void index_mapsFields() {
            service.indexProduct(request("p1", "Widget"));

            verify(repository).save(docCaptor.capture());
            ProductDocument doc = docCaptor.getValue();
            assertThat(doc.getId()).isEqualTo("p1");
            assertThat(doc.getName()).isEqualTo("Widget");
            assertThat(doc.getBrand()).isEqualTo("BrandX");
            assertThat(doc.getMinPrice()).isEqualTo(149.0);
            assertThat(doc.getMaxPrice()).isEqualTo(249.0);
            assertThat(doc.getTotalStock()).isEqualTo(20);
            assertThat(doc.getStatus()).isEqualTo("ACTIVE");
            assertThat(doc.getTags()).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("updates only the status field when the document exists")
        void update_existingDoc() {
            ProductDocument existing = ProductDocument.builder()
                    .id("p1").name("Widget").brand("BrandX").status("ACTIVE").build();
            when(repository.findById("p1")).thenReturn(Optional.of(existing));

            service.updateStatus("p1", "INACTIVE");

            verify(repository).save(docCaptor.capture());
            ProductDocument saved = docCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo("INACTIVE");
            // other fields preserved — no clobbering with nulls
            assertThat(saved.getName()).isEqualTo("Widget");
            assertThat(saved.getBrand()).isEqualTo("BrandX");
        }

        @Test
        @DisplayName("is a no-op when the document is not indexed")
        void update_missingDoc() {
            when(repository.findById("missing")).thenReturn(Optional.empty());

            service.updateStatus("missing", "INACTIVE");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reindexAll()")
    class ReindexAll {

        @Test
        @DisplayName("bulk-saves every request and returns the indexed count")
        void reindex_bulkSaves() {
            List<IndexProductRequest> reqs = List.of(request("p1", "A"), request("p2", "B"));

            int count = service.reindexAll(reqs);

            assertThat(count).isEqualTo(2);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ProductDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(ProductDocument::getId)
                    .containsExactly("p1", "p2");
        }
    }
}
