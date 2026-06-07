package com.hieu.catalog_service.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDTO(
        String id,
        String name,
        String slug,
        String description,
        String categoryId,
        String brand,
        String thumbnail,
        List<String> images,
        String status,
        String metaTitle,
        String metaDescription,
        String metaKeywords,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalStock,
        boolean available,
        List<VariantDTO> variants,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Long version
) {}
