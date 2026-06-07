package com.hieu.auth_service.application.dto;

import java.util.List;

/**
 * Generic pagination envelope — avoids leaking Spring Data's {@code Page} into the API.
 *
 * @param items         items for the current page (never null)
 * @param nextCursor    opaque cursor for the next page; {@code null} when no more pages
 * @param pageSize      size the caller requested
 * @param totalElements total number of matching rows, or {@code -1} if unknown
 */
public record PageDTO<T>(
        List<T> items,
        String nextCursor,
        int pageSize,
        long totalElements
) {
    /** Convenience factory when the cursor is already computed by the caller. */
    public static <T> PageDTO<T> of(List<T> items, String nextCursor, int pageSize, long totalElements) {
        return new PageDTO<>(List.copyOf(items), nextCursor, pageSize, totalElements);
    }
}
