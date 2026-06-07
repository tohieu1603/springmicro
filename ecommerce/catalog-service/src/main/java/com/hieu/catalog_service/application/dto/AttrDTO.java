package com.hieu.catalog_service.application.dto;

import java.util.List;

public record AttrDTO(
        String id,
        String code,
        String name,
        String type,
        int sortOrder,
        List<AttrValDTO> values
) {}
