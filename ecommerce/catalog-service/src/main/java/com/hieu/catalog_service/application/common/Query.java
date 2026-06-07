package com.hieu.catalog_service.application.common;

/**
 * Marker interface for CQRS queries — read-only intents.
 *
 * <p>Queries are immutable value objects (typically Java records) describing a
 * request to read system state without mutating it. Handlers must run under
 * {@code @Transactional(readOnly = true)}.
 *
 * @param <R> result type produced by the handler
 */
public interface Query<R> {
}
