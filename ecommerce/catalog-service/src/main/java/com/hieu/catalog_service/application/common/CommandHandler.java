package com.hieu.catalog_service.application.common;

/**
 * Handles the execution of a single {@link Command} type. Stateless Spring bean per
 * command; depends only on domain ports.
 *
 * @param <C> command type handled
 * @param <R> result type returned to the caller
 */
@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}
