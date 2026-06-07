package com.hieu.common.api;

import java.util.List;

/**
 * Cursor-based pagination envelope shared across services.
 *
 * <p>Unlike offset pagination, cursor pagination uses the last item's anchor for the
 * next page. Gives stable O(log n) lookup regardless of page depth and tolerates
 * concurrent inserts without duplicates/gaps.
 *
 * @param items      rows for the current page (defensive-copied on construction)
 * @param nextCursor opaque cursor for the next page; {@code null} signals last page
 * @param size       page size the caller requested
 * @param hasNext    convenience flag derived from {@code nextCursor != null}
 * @param <T>        item type
 */
public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        int size,
        boolean hasNext
) {
    public CursorPage {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** Convenience factory. */
    public static <T> CursorPage<T> of(List<T> items, String nextCursor, int size) {
        return new CursorPage<>(items, nextCursor, size, nextCursor != null);
    }
}
