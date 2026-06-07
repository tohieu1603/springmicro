package com.hieu.order_service.application.dto;

import java.util.List;

public record CursorPageDTO<T>(
        List<T> content,
        String nextCursor,
        boolean hasMore,
        int size
) {}
