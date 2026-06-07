package com.hieu.catalog_service.application.events;

import java.time.Instant;
import java.util.UUID;

public final class AttrIntegrationEvents {

    private AttrIntegrationEvents() {}

    public record AttrCreated(UUID eventId, Instant occurredOn,
                               String attrId, String code, String name, String type) {}

    public record AttrUpdated(UUID eventId, Instant occurredOn,
                               String attrId, String name) {}

    public record AttrDeleted(UUID eventId, Instant occurredOn, String attrId) {}
}
