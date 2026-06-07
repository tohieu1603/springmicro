package com.hieu.flash_sale_service.dto;

import java.util.List;

/** Generic paginated response. */
public record PageDTO<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
