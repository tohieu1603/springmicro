package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.UpdateProductCommand;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProductHandler — pure unit (orchestration)")
class UpdateProductHandlerTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CatalogDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks UpdateProductHandler handler;

    private final ProductDTO dummyDto = new ProductDTO(
            "1", "x", "x", null, null, null, null, List.of(), "DRAFT",
            null, null, null, null, null, 0, false, List.of(),
            null, null, "tester", "tester", null);

    /** A persisted product (id assigned) so update() can register its event. */
    private Product persistedProduct() {
        Product p = Product.create("Old Name", "old desc", null, "OldBrand", "creator");
        p.assignId(ProductId.of("1"));
        return p;
    }

    @Test
    @DisplayName("product not found → ProductNotFoundException, nothing saved")
    void update_productNotFound_throws() {
        when(productRepository.findByIdWithVariants(ProductId.of("99"))).thenReturn(Optional.empty());
        var cmd = new UpdateProductCommand("99", "New", null, null, null, "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEventsOf(any());
    }

    @Test
    @DisplayName("categoryId set but missing → CategoryNotFoundException, nothing saved")
    void update_unknownCategory_throws() {
        var product = persistedProduct();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(categoryRepository.existsById(CategoryId.of("5"))).thenReturn(false);
        var cmd = new UpdateProductCommand("1", "New", null, "5", null, "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("valid update with known category → applies change, saves, publishes events")
    void update_happyPath_savesAndPublishes() {
        var product = persistedProduct();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(categoryRepository.existsById(CategoryId.of("5"))).thenReturn(true);
        when(productRepository.save(product)).thenReturn(product);
        when(mapper.toDto(product)).thenReturn(dummyDto);
        var cmd = new UpdateProductCommand("1", "New Name", "new desc", "5", "NewBrand", "editor");

        var result = handler.handle(cmd);

        assertThat(result).isSameAs(dummyDto);
        assertThat(product.getName()).isEqualTo("New Name");
        assertThat(product.getCategoryId()).isEqualTo(CategoryId.of("5"));
        verify(productRepository).save(product);
        verify(eventPublisher).publishEventsOf(product);
    }

    @Test
    @DisplayName("null categoryId → category repo not consulted, update still applied")
    void update_nullCategory_skipsCategoryCheck() {
        var product = persistedProduct();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(mapper.toDto(product)).thenReturn(dummyDto);
        // categoryRepository stubs intentionally absent — STRICT_STUBS proves it isn't touched.
        lenient().when(categoryRepository.existsById(any())).thenReturn(true);
        var cmd = new UpdateProductCommand("1", "Renamed", null, null, null, "editor");

        handler.handle(cmd);

        assertThat(product.getName()).isEqualTo("Renamed");
        verify(categoryRepository, never()).existsById(any());
        verify(eventPublisher).publishEventsOf(product);
    }
}
