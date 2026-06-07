package com.hieu.catalog_service.application.dto;

public record AttrValDTO(
        String id,
        String attrId,
        String val,
        String code,
        int sortOrder
) {}
