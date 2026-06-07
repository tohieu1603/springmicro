package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.command.variant.UpdateVariantStockCommand;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateVariantStockHandler — pure unit (orchestration)")
class UpdateVariantStockHandlerTest {

    @Mock ProductRepository productRepository;
    @Mock CatalogDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks UpdateVariantStockHandler handler;

    private final VariantDTO dummyDto = new VariantDTO(
            "100", "1", "SKU-1", new BigDecimal("10.00"), null, null,
            new BigDecimal("10.00"), null, null, 25, "ACTIVE", true, List.of());

    /** Persisted product whose single variant has id "100" (so updateVariantStock resolves it). */
    private static Product persistedProductWithVariant() {
        Variant v = Variant.reconstitute(VariantId.of("100"), ProductId.of("1"), Sku.of("SKU-1"),
                Money.of(new BigDecimal("10.00")), null, null, null, new BigDecimal("0.5"),
                Quantity.of(5), com.hieu.catalog_service.domain.model.product.valueobject.VariantStatus.ACTIVE,
                null, List.of());
        return Product.reconstitute(ProductId.of("1"), "Prod",
                com.hieu.catalog_service.domain.model.product.valueobject.Slug.of("prod"), "desc",
                null, "Brand", null, List.of(),
                com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus.ACTIVE,
                null, null, null, java.time.Instant.now(), java.time.Instant.now(),
                "creator", "creator", 0L, List.of(v));
    }

    @Test
    @DisplayName("product not found → ProductNotFoundException, nothing saved")
    void update_productNotFound_throws() {
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.empty());
        var cmd = new UpdateVariantStockCommand("1", "100", 25, "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEventsOf(any());
    }

    @Test
    @DisplayName("variant id not on product → IllegalArgumentException from aggregate")
    void update_unknownVariant_throws() {
        var product = persistedProductWithVariant();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        var cmd = new UpdateVariantStockCommand("1", "999", 25, "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(IllegalArgumentException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("valid → stock replaced, saved, events published, mapped DTO returned")
    void update_happyPath() {
        var product = persistedProductWithVariant();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(mapper.toDto(any(Variant.class))).thenReturn(dummyDto);
        var cmd = new UpdateVariantStockCommand("1", "100", 25, "tester");

        var result = handler.handle(cmd);

        assertThat(result).isSameAs(dummyDto);
        assertThat(product.getVariants().get(0).getQuantity().value()).isEqualTo(25);
        verify(productRepository).save(product);
        verify(eventPublisher).publishEventsOf(product);
    }
}
