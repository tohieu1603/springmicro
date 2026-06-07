package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        String categoryId,
        String brand,
        @Pattern(regexp = "^https?://[\\w\\-./%?=&:#]+$", message = "thumbnail must be http(s) URL")
        String thumbnail,
        List<@Pattern(regexp = "^https?://[\\w\\-./%?=&:#]+$", message = "image must be http(s) URL") String> images,
        String metaTitle,
        String metaDescription,
        String metaKeywords,
        @NotEmpty @Valid List<VariantRequest> variants,
        // Nullable wrapper so missing JSON field defaults to null (stays DRAFT) instead of
        // Jackson throwing MismatchedInputException on primitive deserialisation.
        Boolean activate
) {
    /** Resolve the nullable flag — missing or {@code null} means "do not activate". */
    public boolean activateOrFalse() { return Boolean.TRUE.equals(activate); }

    public record VariantRequest(
            @NotBlank String sku,
            @NotNull @PositiveOrZero BigDecimal price,
            BigDecimal cost,
            BigDecimal salePrice,
            String image,
            BigDecimal weight,
            @PositiveOrZero int quantity,
            List<AttrRequest> attrs
    ) {}

    public record AttrRequest(
            @NotNull String attrId,
            String attrValId,
            String valText
    ) {}
}
