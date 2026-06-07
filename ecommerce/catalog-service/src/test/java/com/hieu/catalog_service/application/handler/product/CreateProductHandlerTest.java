package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.CreateProductCommand;
import com.hieu.catalog_service.application.command.product.CreateProductCommand.AttrCmd;
import com.hieu.catalog_service.application.command.product.CreateProductCommand.VariantCmd;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.common.ValidationException;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.exception.AttrValNotFoundException;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.exception.ProductAlreadyExistsException;
import com.hieu.catalog_service.domain.exception.VariantSkuAlreadyExistsException;
import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductHandler — pure unit (orchestration)")
class CreateProductHandlerTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock AttrRepository attrRepository;
    @Mock CatalogDtoMapper mapper;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks CreateProductHandler handler;

    private final ProductDTO dummyDto = new ProductDTO(
            "1", "x", "x", null, null, null, null, List.of(), "DRAFT",
            null, null, null, null, null, 0, false, List.of(),
            null, null, "tester", "tester", null);

    @BeforeEach
    void persistOnSave() {
        // The handler assigns ids itself? No — Product.create() leaves id null, but
        // raiseCreatedEvent()/publishEventsOf() require a persisted aggregate. Repository
        // save returns a hydrated aggregate; emulate that by assigning ids on the way out.
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            persist(p);
            return p;
        });
        lenient().when(mapper.toDto(any(Product.class))).thenReturn(dummyDto);
    }

    /** Assign sequential ids the way the JPA adapter would after a flush. */
    private static void persist(Product p) {
        if (p.getId() == null) {
            p.assignId(com.hieu.catalog_service.domain.model.product.valueobject.ProductId.of("1"));
        }
        String[] variantIds = {"100", "101", "102", "103", "104", "105"};
        int vi = 0;
        for (var v : p.getVariants()) {
            if (v.getId() == null) {
                v.assignId(com.hieu.catalog_service.domain.model.product.valueobject.VariantId.of(variantIds[vi++]));
            }
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static VariantCmd variant(String sku, List<AttrCmd> attrs) {
        return new VariantCmd(sku, new BigDecimal("199.99"), null, null, null,
                new BigDecimal("0.5"), 10, attrs);
    }

    private static CreateProductCommand command(String name, String categoryId, List<VariantCmd> variants) {
        return new CreateProductCommand(name, "desc", categoryId, "Brand",
                null, null, null, null, null, variants, false, "tester");
    }

    private static Attr selectAttr(String id, AttrVal... vals) {
        return Attr.reconstitute(AttrId.of(id), "COLOR", "Colour", AttrType.SELECT, 0, List.of(vals));
    }

    private static Attr textAttr(String id) {
        return Attr.reconstitute(AttrId.of(id), "MATERIAL", "Material", AttrType.TEXT, 0, List.of());
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("creates product, raises created event and publishes events of the saved aggregate")
    void create_happyPath_publishesEvents() {
        var cmd = command("Cool Shirt", null, List.of(variant("SKU-1", List.of())));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(false);

        var result = handler.handle(cmd);

        assertThat(result).isSameAs(dummyDto);
        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        // raiseCreatedEvent() ran => a ProductCreatedEvent must be buffered before publish.
        verify(eventPublisher).publishEventsOf(savedCaptor.getValue());
        assertThat(savedCaptor.getValue().getVariants()).hasSize(1);
    }

    @Test
    @DisplayName("activate=true → product saved in ACTIVE status (variant present so activation allowed)")
    void create_withActivate_isActive() {
        var cmd = new CreateProductCommand("Active Prod", "d", null, "B",
                null, null, null, null, null, List.of(variant("SKU-ACT", List.of())), true, "tester");
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(false);

        handler.handle(cmd);

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isActive()).isTrue();
    }

    // ── validation ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("blank name → ValidationException with field error, nothing saved")
    void create_blankName_validationException() {
        var cmd = command("  ", null, List.of(variant("SKU-1", List.of())));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).fieldErrors()).containsKey("name"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("no variants → ValidationException, nothing saved")
    void create_noVariants_validationException() {
        var cmd = command("Has No Variants", null, List.of());

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).fieldErrors()).containsKey("variants"));
        verify(productRepository, never()).save(any());
    }

    // ── category existence ──────────────────────────────────────────────────

    @Test
    @DisplayName("categoryId set but not found → CategoryNotFoundException")
    void create_unknownCategory_throws() {
        var cmd = command("Cat Prod", "7", List.of(variant("SKU-1", List.of())));
        when(categoryRepository.existsById(CategoryId.of("7"))).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    // ── attribute resolution ──────────────────────────────────────────────────

    @Test
    @DisplayName("SELECT attr without attrValId → ValidationException")
    void create_selectAttrMissingValId_throws() {
        var attr = selectAttr("5", AttrVal.reconstitute("50", "5", "Red", "RED", 0));
        var cmd = command("Sel Prod", null,
                List.of(variant("SKU-1", List.of(new AttrCmd("5", null, null)))));
        when(attrRepository.findAllByIdsWithValues(List.of("5"))).thenReturn(List.of(attr));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SELECT");
    }

    @Test
    @DisplayName("SELECT attr with unknown attrValId → AttrValNotFoundException")
    void create_selectAttrUnknownValId_throws() {
        var attr = selectAttr("5", AttrVal.reconstitute("50", "5", "Red", "RED", 0));
        var cmd = command("Sel Prod", null,
                List.of(variant("SKU-1", List.of(new AttrCmd("5", "999", null)))));
        when(attrRepository.findAllByIdsWithValues(List.of("5"))).thenReturn(List.of(attr));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(AttrValNotFoundException.class);
    }

    @Test
    @DisplayName("TEXT attr with blank valText → ValidationException")
    void create_textAttrBlank_throws() {
        var attr = textAttr("8");
        var cmd = command("Text Prod", null,
                List.of(variant("SKU-1", List.of(new AttrCmd("8", null, "   ")))));
        when(attrRepository.findAllByIdsWithValues(List.of("8"))).thenReturn(List.of(attr));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("valText is required");
    }

    @Test
    @DisplayName("referenced attrId not loaded → AttrNotFoundException")
    void create_unknownAttr_throws() {
        var cmd = command("Bad Attr Prod", null,
                List.of(variant("SKU-1", List.of(new AttrCmd("42", "1", null)))));
        when(attrRepository.findAllByIdsWithValues(List.of("42"))).thenReturn(List.of());

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(AttrNotFoundException.class);
    }

    @Test
    @DisplayName("valid SELECT + TEXT attrs resolve and product is saved")
    void create_validAttrs_saves() {
        var sel = selectAttr("5", AttrVal.reconstitute("50", "5", "Red", "RED", 0));
        var txt = textAttr("8");
        var cmd = command("Multi Attr", null, List.of(
                variant("SKU-1", List.of(new AttrCmd("5", "50", null), new AttrCmd("8", null, "Cotton")))));
        when(attrRepository.findAllByIdsWithValues(List.of("5", "8"))).thenReturn(List.of(sel, txt));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(false);

        handler.handle(cmd);

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getVariants().get(0).getAttrs()).hasSize(2);
    }

    // ── SKU duplicate (pre-check) ──────────────────────────────────────────────

    @Test
    @DisplayName("existsBySku true → VariantSkuAlreadyExistsException, nothing saved")
    void create_duplicateSku_throws() {
        var cmd = command("Dup Sku", null, List.of(variant("SKU-DUP", List.of())));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(VariantSkuAlreadyExistsException.class);
        verify(productRepository, never()).save(any());
    }

    // ── slug uniqueness ───────────────────────────────────────────────────────

    @Test
    @DisplayName("base slug taken → handler appends suffix and still saves")
    void create_slugCollision_appendsSuffix() {
        var cmd = command("Taken Name", null, List.of(variant("SKU-1", List.of())));
        // base slug exists, suffixed slug free
        when(productRepository.existsBySlug(any())).thenReturn(true, false);
        when(productRepository.existsBySku(any())).thenReturn(false);

        handler.handle(cmd);

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        // suffix appended: "taken-name-XXXXXXXX"
        assertThat(savedCaptor.getValue().getSlug().value())
                .startsWith("taken-name-")
                .hasSizeGreaterThan("taken-name".length());
    }

    @Test
    @DisplayName("both base and suffixed slug taken → ProductAlreadyExistsException")
    void create_slugAndSuffixTaken_throws() {
        var cmd = command("Taken Name", null, List.of(variant("SKU-1", List.of())));
        when(productRepository.existsBySlug(any())).thenReturn(true, true);

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ProductAlreadyExistsException.class);
        verify(productRepository, never()).save(any());
    }

    // ── DataIntegrityViolation translation ──────────────────────────────────────

    @Test
    @DisplayName("DataIntegrityViolation mentioning sku → VariantSkuAlreadyExistsException")
    void create_dbViolationSku_translated() {
        var cmd = command("Race Prod", null, List.of(variant("SKU-1", List.of())));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(false);
        when(productRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint uq_variant_sku"));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(VariantSkuAlreadyExistsException.class);
        verify(eventPublisher, never()).publishEventsOf(any());
    }

    @Test
    @DisplayName("DataIntegrityViolation mentioning slug → ProductAlreadyExistsException")
    void create_dbViolationSlug_translated() {
        var cmd = command("Race Prod", null, List.of(variant("SKU-1", List.of())));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(false);
        when(productRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint uq_product_slug"));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    @DisplayName("DataIntegrityViolation with no recognised token → rethrown as-is")
    void create_dbViolationUnknown_rethrown() {
        var cmd = command("Race Prod", null, List.of(variant("SKU-1", List.of())));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.existsBySku(any())).thenReturn(false);
        when(productRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
