package com.hieu.catalog_service.application.common;

/**
 * Marker interface for CQRS commands — state-mutating intents.
 *
 * <p>Commands are immutable value objects (typically Java records) describing a
 * request to change system state. Each {@code Command} is paired with exactly one
 * {@link CommandHandler} implementation. The generic parameter {@code R} names the
 * handler's return type; use {@link Void} when the command produces no result.
 *
 * @param <R> result type produced by the handler
 */
public interface Command<R> {
}
