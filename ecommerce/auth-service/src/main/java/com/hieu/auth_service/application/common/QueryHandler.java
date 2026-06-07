package com.hieu.auth_service.application.common;

/**
 * Handles the execution of a single {@link Query} type.
 *
 * <p>Implementations are stateless Spring beans registered in the application layer.
 * Handlers must not mutate state and are expected to run under
 * {@code @Transactional(readOnly = true)}.
 *
 * @param <Q> query type handled
 * @param <R> result type returned to the caller
 */
@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

    /**
     * Executes the query and returns its result.
     *
     * @param query non-null query to execute
     * @return handler result; may be {@code null} when not found
     */
    R handle(Q query);
}
