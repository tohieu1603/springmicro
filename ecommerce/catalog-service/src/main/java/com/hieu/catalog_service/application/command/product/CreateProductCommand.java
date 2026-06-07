package com.hieu.catalog_service.application.command.product;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.ProductDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create a product with one or more initial variants.
 *
 * <p>The command carries everything needed to persist the full aggregate in one
 * transaction so downstream consumers (inventory-service via {@code ProductCreatedEvent})
 * receive a single, complete snapshot instead of a partial then a patch.
 *
 * @param name          display name; used to derive the slug
 * @param description   long description
 * @param categoryId    optional category assignment (UUID string)
 * @param brand         optional brand label
 * @param thumbnail     primary image URL
 * @param images        gallery image URLs
 * @param metaTitle     SEO title
 * @param metaDescription SEO description
 * @param metaKeywords  comma-separated keywords
 * @param variants      at least one variant — a product with no variants cannot be sold
 * @param activate      start in {@link com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus#ACTIVE}
 *                      instead of {@code DRAFT}
 * @param createdBy     user id pulled from the JWT principal in the controller
 */
public record CreateProductCommand(
        String name,
        String description,
        String categoryId,
        String brand,
        String thumbnail,
        List<String> images,
        String metaTitle,
        String metaDescription,
        String metaKeywords,
        List<VariantCmd> variants,
        boolean activate,
        String createdBy
) implements Command<ProductDTO> {

    /**
     * Nested variant payload. {@code attrs} captures the dimensions (e.g. Color = Red,
     * Size = XL); for {@code SELECT} attrs supply {@code attrValId}, for {@code TEXT}/{@code NUMBER}
     * supply {@code valText}.
     */
    public record VariantCmd(
            String sku,
            BigDecimal price,
            BigDecimal cost,
            BigDecimal salePrice,
            String image,
            BigDecimal weight,
            int quantity,
            List<AttrCmd> attrs
    ) {}

    public record AttrCmd(String attrId, String attrValId, String valText) {}
}
