package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * sortOrder is boxed Integer + compact ctor defaults to 0 — Jackson 3 rejects payloads
 * that omit primitive int fields (no implicit zero coercion). Box + default avoids that.
 */
public record CategoryRequest(
        @NotBlank String name,
        String description,
        String parentId,
        Integer sortOrder
) {
    public CategoryRequest {
        if (sortOrder == null) sortOrder = 0;
    }

    /** Convenience accessor for handlers that still expect a primitive. */
    public int sortOrderValue() {
        return sortOrder;
    }
}
