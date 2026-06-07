package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AddAttrValueRequest(@NotBlank String val, String code) {}
