package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.command.variant.RemoveVariantCommand;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.Slug;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantStatus;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveVariantHandler — pure unit (orchestration)")
class RemoveVariantHandlerTest {

    @Mock ProductRepository productRepository;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks RemoveVariantHandler handler;

    private static Variant variant(String id, String sku) {
        return Variant.reconstitute(VariantId.of(id), ProductId.of("1"), Sku.of(sku),
                Money.of(new BigDecimal("10.00")), null, null, null, new BigDecimal("0.5"),
                Quantity.of(5), VariantStatus.ACTIVE, null, List.of());
    }

    /** Persisted ACTIVE product with two variants (so one can be removed). */
    private static Product productWithTwoVariants() {
        return Product.reconstitute(ProductId.of("1"), "Prod", Slug.of("prod"), "desc",
                null, "Brand", null, List.of(), ProductStatus.ACTIVE,
                null, null, null, Instant.now(), Instant.now(),
                "creator", "creator", 0L, List.of(variant("100", "SKU-1"), variant("200", "SKU-2")));
    }

    @Test
    @DisplayName("product not found → ProductNotFoundException, nothing saved")
    void remove_productNotFound_throws() {
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.empty());
        var cmd = new RemoveVariantCommand("1", "100", "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEventsOf(any());
    }

    @Test
    @DisplayName("unknown variant id → IllegalArgumentException from aggregate, nothing saved")
    void remove_unknownVariant_throws() {
        var product = productWithTwoVariants();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        var cmd = new RemoveVariantCommand("1", "999", "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(IllegalArgumentException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("last variant of ACTIVE product → IllegalStateException, nothing saved")
    void remove_lastVariantOfActive_throws() {
        Product product = Product.reconstitute(ProductId.of("1"), "Prod", Slug.of("prod"), "desc",
                null, "Brand", null, List.of(), ProductStatus.ACTIVE,
                null, null, null, Instant.now(), Instant.now(),
                "creator", "creator", 0L, List.of(variant("100", "SKU-1")));
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        var cmd = new RemoveVariantCommand("1", "100", "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(IllegalStateException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("valid removal → variant gone, saved, events published, returns null")
    void remove_happyPath() {
        var product = productWithTwoVariants();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        var cmd = new RemoveVariantCommand("1", "100", "tester");

        var result = handler.handle(cmd);

        assertThat(result).isNull();
        assertThat(product.getVariants()).noneMatch(v -> v.getId().equals(VariantId.of("100")));
        assertThat(product.getVariants()).hasSize(1);
        verify(productRepository).save(product);
        verify(eventPublisher).publishEventsOf(product);
    }
}
