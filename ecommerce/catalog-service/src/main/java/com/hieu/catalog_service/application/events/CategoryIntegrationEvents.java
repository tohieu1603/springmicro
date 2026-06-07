package com.hieu.catalog_service.application.events;

import java.time.Instant;
import java.util.UUID;

public final class CategoryIntegrationEvents {

    private CategoryIntegrationEvents() {}

    public record CategoryCreated(UUID eventId, Instant occurredOn,
                                   String categoryId, String name, String parentId, String createdBy) {}

    public record CategoryUpdated(UUID eventId, Instant occurredOn,
                                   String categoryId, String name, String parentId, String updatedBy) {}

    public record CategoryDeleted(UUID eventId, Instant occurredOn,
                                   String categoryId, String deletedBy) {}
}
