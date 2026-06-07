package com.hieu.search_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.search_service.document.ProductDocument;
import com.hieu.search_service.dto.IndexProductRequest;
import com.hieu.search_service.dto.PageResponse;
import com.hieu.search_service.dto.SearchRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link SearchController}: the service is mocked and the returned
 * {@link ResponseEntity} status + {@link ApiResponse} envelope are asserted, plus the
 * query-param -> {@link SearchRequest} mapping in {@code searchByParams}. No MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController (unit)")
class SearchControllerTest {

    @Mock SearchApplicationService searchService;
    @InjectMocks SearchController controller;

    @Captor ArgumentCaptor<SearchRequest> reqCaptor;

    private static PageResponse<ProductDocument> page() {
        return PageResponse.<ProductDocument>builder()
                .content(List.of(ProductDocument.builder().id("p1").name("Widget").build()))
                .totalElements(1).totalPages(1).page(0).size(20).build();
    }

    @Nested
    @DisplayName("searchByBody()")
    class SearchByBody {

        @Test
        @DisplayName("delegates the body request and wraps the page in an ok envelope")
        void delegatesAndWraps() {
            SearchRequest body = SearchRequest.builder().q("phone").build();
            PageResponse<ProductDocument> page = page();
            when(searchService.searchProducts(body)).thenReturn(page);

            ResponseEntity<ApiResponse<PageResponse<ProductDocument>>> resp =
                    controller.searchByBody(body);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().success()).isTrue();
            assertThat(resp.getBody().data()).isSameAs(page);
            verify(searchService).searchProducts(body);
        }
    }

    @Nested
    @DisplayName("searchByParams()")
    class SearchByParams {

        @Test
        @DisplayName("maps every query param onto the built SearchRequest")
        void mapsAllParams() {
            when(searchService.searchProducts(any(SearchRequest.class))).thenReturn(page());

            ResponseEntity<ApiResponse<PageResponse<ProductDocument>>> resp =
                    controller.searchByParams("phone", "Acme", "cat-1",
                            10.0, 99.0, "ACTIVE", "price", 2, 50);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(searchService).searchProducts(reqCaptor.capture());
            SearchRequest req = reqCaptor.getValue();
            assertThat(req.getQ()).isEqualTo("phone");
            assertThat(req.getBrand()).isEqualTo("Acme");
            assertThat(req.getCategoryId()).isEqualTo("cat-1");
            assertThat(req.getMinPrice()).isEqualTo(10.0);
            assertThat(req.getMaxPrice()).isEqualTo(99.0);
            assertThat(req.getStatus()).isEqualTo("ACTIVE");
            assertThat(req.getSortBy()).isEqualTo("price");
            assertThat(req.getPage()).isEqualTo(2);
            assertThat(req.getSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("passes nulls through and keeps pagination values")
        void mapsNullsAndPagination() {
            when(searchService.searchProducts(any(SearchRequest.class))).thenReturn(page());

            controller.searchByParams(null, null, null, null, null, null, null, 0, 20);

            verify(searchService).searchProducts(reqCaptor.capture());
            SearchRequest req = reqCaptor.getValue();
            assertThat(req.getQ()).isNull();
            assertThat(req.getBrand()).isNull();
            assertThat(req.getMinPrice()).isNull();
            assertThat(req.getMaxPrice()).isNull();
            assertThat(req.getSortBy()).isNull();
            assertThat(req.getPage()).isZero();
            assertThat(req.getSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("suggest()")
    class Suggest {

        @Test
        @DisplayName("delegates query+size and wraps the suggestion list in an ok envelope")
        void delegatesSuggest() {
            when(searchService.getSuggestions("pho", 5)).thenReturn(List.of("phone", "photo"));

            ResponseEntity<ApiResponse<List<String>>> resp = controller.suggest("pho", 5);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().success()).isTrue();
            assertThat(resp.getBody().data()).containsExactly("phone", "photo");
            verify(searchService).getSuggestions("pho", 5);
        }
    }

    @Nested
    @DisplayName("indexProduct()")
    class IndexProduct {

        @Test
        @DisplayName("invokes the service and returns ok envelope with confirmation message")
        void indexesAndConfirms() {
            IndexProductRequest req = IndexProductRequest.builder().id("p1").name("Widget").build();

            ResponseEntity<ApiResponse<Void>> resp = controller.indexProduct(req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().success()).isTrue();
            assertThat(resp.getBody().message()).isEqualTo("Product indexed");
            verify(searchService).indexProduct(req);
        }
    }

    @Nested
    @DisplayName("removeProduct()")
    class RemoveProduct {

        @Test
        @DisplayName("invokes the service and returns ok envelope with scheduled message")
        void removesAndConfirms() {
            ResponseEntity<ApiResponse<Void>> resp = controller.removeProduct("p9");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().success()).isTrue();
            assertThat(resp.getBody().message()).isEqualTo("Product removal scheduled");
            verify(searchService).removeProduct("p9");
        }
    }

    @Nested
    @DisplayName("reindexAll()")
    class ReindexAll {

        @Test
        @DisplayName("returns the indexed count map produced by the service")
        void returnsCountMap() {
            List<IndexProductRequest> reqs =
                    List.of(IndexProductRequest.builder().id("p1").build(),
                            IndexProductRequest.builder().id("p2").build());
            when(searchService.reindexAll(reqs)).thenReturn(2);

            ResponseEntity<Map<String, Object>> resp = controller.reindexAll(reqs);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody()).containsEntry("indexed", 2);
            verify(searchService).reindexAll(eq(reqs));
        }
    }
}
