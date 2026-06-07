package com.hieu.auth_service.application.common;

/**
 * Handles the execution of a single {@link Command} type.
 *
 * <p>Implementations are stateless Spring beans registered in the application layer.
 * One handler per command preserves the single-responsibility rule and enables
 * easy unit testing — handlers depend only on domain ports, never on web frameworks.
 *
 * @param <C> command type handled
 * @param <R> result type returned to the caller
 */
@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {

    /**
     * Executes the command and returns its result.
     *
     * @param command non-null command to execute
     * @return handler result; may be {@code null} when {@code R} is {@link Void}
     */
    R handle(C command);
}
