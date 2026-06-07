package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.ChangeProductStatusCommand;
import com.hieu.catalog_service.application.command.product.ChangeProductStatusCommand.Transition;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeProductStatusHandler — pure unit (orchestration)")
class ChangeProductStatusHandlerTest {

    @Mock ProductRepository productRepository;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks ChangeProductStatusHandler handler;

    /** Persisted DRAFT product carrying one variant (so activation is allowed). */
    private static Product draftWithVariant() {
        Product p = Product.create("Prod", "desc", null, "Brand", "creator");
        p.assignId(ProductId.of("1"));
        Variant v = Variant.create(Sku.of("SKU-1"), Money.of(new BigDecimal("10.00")),
                null, null, null, null, Quantity.of(5));
        p.addVariant(v);
        return p;
    }

    @Test
    @DisplayName("product not found → ProductNotFoundException, nothing saved")
    void changeStatus_notFound_throws() {
        when(productRepository.findByIdWithVariants(ProductId.of("7"))).thenReturn(Optional.empty());
        var cmd = new ChangeProductStatusCommand("7", Transition.ACTIVATE, "tester");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEventsOf(any());
    }

    @Test
    @DisplayName("ACTIVATE → status ACTIVE, saved, events published")
    void changeStatus_activate() {
        var product = draftWithVariant();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        var cmd = new ChangeProductStatusCommand("1", Transition.ACTIVATE, "tester");

        var result = handler.handle(cmd);

        assertThat(result).isNull();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        verify(productRepository).save(product);
        verify(eventPublisher).publishEventsOf(product);
    }

    @Test
    @DisplayName("DEACTIVATE → status INACTIVE")
    void changeStatus_deactivate() {
        var product = draftWithVariant();
        product.activate("creator"); // ACTIVE first
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        var cmd = new ChangeProductStatusCommand("1", Transition.DEACTIVATE, "tester");

        handler.handle(cmd);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        verify(eventPublisher).publishEventsOf(product);
    }

    @Test
    @DisplayName("DRAFT → status DRAFT (from INACTIVE)")
    void changeStatus_toDraft() {
        var product = draftWithVariant();
        product.activate("creator");
        product.deactivate("creator"); // now INACTIVE
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        var cmd = new ChangeProductStatusCommand("1", Transition.DRAFT, "tester");

        handler.handle(cmd);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        verify(eventPublisher).publishEventsOf(product);
    }
}
