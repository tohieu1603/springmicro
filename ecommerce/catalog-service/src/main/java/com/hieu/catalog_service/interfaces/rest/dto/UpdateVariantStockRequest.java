package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateVariantStockRequest(@PositiveOrZero int quantity) {}
