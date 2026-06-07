package com.hieu.order_service.application.common;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
