package com.hieu.catalog_service.application.dto;

public record VariantAttrDTO(
        String id,
        String attrId,
        String attrCode,
        String attrName,
        String valId,
        String valText
) {}
