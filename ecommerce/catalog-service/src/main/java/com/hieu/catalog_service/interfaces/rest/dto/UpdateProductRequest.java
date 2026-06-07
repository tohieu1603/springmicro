package com.hieu.catalog_service.interfaces.rest.dto;

public record UpdateProductRequest(
        String name,
        String description,
        String categoryId,
        String brand
) {}
