package com.hieu.notification_service.dto;

import java.util.List;

/** Offset-based pagination envelope. */
public record PageDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {}
