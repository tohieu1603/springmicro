package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateAttrRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String type,          // SELECT | TEXT | NUMBER
        List<ValueRequest> values
) {
    public record ValueRequest(@NotBlank String val, String code) {}
}
