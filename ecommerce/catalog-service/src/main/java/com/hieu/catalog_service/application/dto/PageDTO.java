package com.hieu.catalog_service.application.dto;

import java.util.List;

/**
 * Generic pagination envelope — keeps Spring Data's {@code Page} out of the API contract.
 *
 * @param items         items on the current page (never null)
 * @param nextCursor    opaque cursor for the next page; {@code null} when no more
 * @param pageSize      size the caller requested
 * @param totalElements total matching rows, or {@code -1} when unknown
 */
public record PageDTO<T>(
        List<T> items,
        String nextCursor,
        int pageSize,
        long totalElements
) {
    public static <T> PageDTO<T> of(List<T> items, String nextCursor, int pageSize, long totalElements) {
        return new PageDTO<>(List.copyOf(items), nextCursor, pageSize, totalElements);
    }
}
