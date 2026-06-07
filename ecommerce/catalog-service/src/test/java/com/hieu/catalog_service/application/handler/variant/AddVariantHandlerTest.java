package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.command.product.CreateProductCommand.AttrCmd;
import com.hieu.catalog_service.application.command.variant.AddVariantCommand;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.common.ValidationException;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.exception.AttrValNotFoundException;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.exception.VariantSkuAlreadyExistsException;
import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.repository.AttrRepository;
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
@DisplayName("AddVariantHandler — pure unit (orchestration)")
class AddVariantHandlerTest {

    @Mock ProductRepository productRepository;
    @Mock AttrRepository attrRepository;
    @Mock CatalogDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks AddVariantHandler handler;

    private final VariantDTO dummyDto = new VariantDTO(
            "100", "1", "SKU-NEW", new BigDecimal("10.00"), null, null,
            new BigDecimal("10.00"), null, null, 3, "ACTIVE", true, List.of());

    /** Persisted product with one existing variant. */
    private static Product persistedProduct() {
        Product p = Product.create("Prod", "desc", null, "Brand", "creator");
        p.assignId(ProductId.of("1"));
        Variant existing = Variant.create(Sku.of("SKU-OLD"), Money.of(new BigDecimal("5.00")),
                null, null, null, null, Quantity.of(2));
        p.addVariant(existing);
        return p;
    }

    private static AddVariantCommand command(String sku, List<AttrCmd> attrs) {
        return new AddVariantCommand("1", sku, new BigDecimal("10.00"), null, null,
                null, new BigDecimal("0.5"), 3, attrs, "tester");
    }

    @Test
    @DisplayName("product not found → ProductNotFoundException")
    void add_productNotFound_throws() {
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(command("SKU-NEW", List.of())))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("existsBySku true → VariantSkuAlreadyExistsException, nothing saved")
    void add_duplicateSku_throws() {
        var product = persistedProduct();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(command("SKU-NEW", List.of())))
                .isInstanceOf(VariantSkuAlreadyExistsException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("no attrs → variant added, saved, events published, mapped DTO returned")
    void add_happyPath_noAttrs() {
        var product = persistedProduct();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);
        when(mapper.toDto(any(Variant.class))).thenReturn(dummyDto);

        var result = handler.handle(command("SKU-NEW", List.of()));

        assertThat(result).isSameAs(dummyDto);
        assertThat(product.getVariants()).anyMatch(v -> v.getSku().equals(Sku.of("SKU-NEW")));
        verify(eventPublisher).publishEventsOf(product);
    }

    @Test
    @DisplayName("SELECT attr without attrValId → ValidationException")
    void add_selectAttrMissingValId_throws() {
        var product = persistedProduct();
        var attr = Attr.reconstitute(AttrId.of("5"), "COLOR", "Colour", AttrType.SELECT, 0,
                List.of(AttrVal.reconstitute("50", "5", "Red", "RED", 0)));
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(false);
        when(attrRepository.findById(AttrId.of("5"))).thenReturn(Optional.of(attr));

        assertThatThrownBy(() -> handler.handle(command("SKU-NEW", List.of(new AttrCmd("5", null, null)))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SELECT");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("SELECT attr with unknown attrValId → AttrValNotFoundException")
    void add_selectAttrUnknownValId_throws() {
        var product = persistedProduct();
        var attr = Attr.reconstitute(AttrId.of("5"), "COLOR", "Colour", AttrType.SELECT, 0,
                List.of(AttrVal.reconstitute("50", "5", "Red", "RED", 0)));
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(false);
        when(attrRepository.findById(AttrId.of("5"))).thenReturn(Optional.of(attr));

        assertThatThrownBy(() -> handler.handle(command("SKU-NEW", List.of(new AttrCmd("5", "999", null)))))
                .isInstanceOf(AttrValNotFoundException.class);
    }

    @Test
    @DisplayName("TEXT attr with blank valText → ValidationException")
    void add_textAttrBlank_throws() {
        var product = persistedProduct();
        var attr = Attr.reconstitute(AttrId.of("8"), "MATERIAL", "Material", AttrType.TEXT, 0, List.of());
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(false);
        when(attrRepository.findById(AttrId.of("8"))).thenReturn(Optional.of(attr));

        assertThatThrownBy(() -> handler.handle(command("SKU-NEW", List.of(new AttrCmd("8", null, " ")))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("valText is required");
    }

    @Test
    @DisplayName("unknown attrId → AttrNotFoundException")
    void add_unknownAttr_throws() {
        var product = persistedProduct();
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(false);
        when(attrRepository.findById(AttrId.of("42"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(command("SKU-NEW", List.of(new AttrCmd("42", "1", null)))))
                .isInstanceOf(AttrNotFoundException.class);
    }

    @Test
    @DisplayName("valid SELECT attr → variant carries resolved attr, saved")
    void add_validSelectAttr_saves() {
        var product = persistedProduct();
        var attr = Attr.reconstitute(AttrId.of("5"), "COLOR", "Colour", AttrType.SELECT, 0,
                List.of(AttrVal.reconstitute("50", "5", "Red", "RED", 0)));
        when(productRepository.findByIdWithVariants(ProductId.of("1"))).thenReturn(Optional.of(product));
        when(productRepository.existsBySku(Sku.of("SKU-NEW"))).thenReturn(false);
        when(attrRepository.findById(AttrId.of("5"))).thenReturn(Optional.of(attr));
        when(productRepository.save(product)).thenReturn(product);
        when(mapper.toDto(any(Variant.class))).thenReturn(dummyDto);

        handler.handle(command("SKU-NEW", List.of(new AttrCmd("5", "50", null))));

        var added = product.getVariants().stream()
                .filter(v -> v.getSku().equals(Sku.of("SKU-NEW"))).findFirst().orElseThrow();
        assertThat(added.getAttrs()).hasSize(1);
        assertThat(added.getAttrs().get(0).getValText()).isEqualTo("Red");
        verify(eventPublisher).publishEventsOf(product);
    }
}
