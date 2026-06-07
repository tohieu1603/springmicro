package com.hieu.order_service.application.common;

@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}
