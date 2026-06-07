package com.hieu.catalog_service.application.common;

/**
 * Handles the execution of a single {@link Query} type. Handlers must not mutate state
 * and are expected to run under {@code @Transactional(readOnly = true)}.
 *
 * @param <Q> query type handled
 * @param <R> result type returned to the caller
 */
@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
