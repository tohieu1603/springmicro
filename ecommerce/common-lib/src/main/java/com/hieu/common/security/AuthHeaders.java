package com.hieu.common.security;

/**
 * Standard HTTP header names used to propagate identity between gateway and services.
 *
 * <p>Downstream services can trust these headers <em>only</em> when they originate from
 * the gateway (authenticated by JWT). Never accept them from external callers.
 */
public final class AuthHeaders {
    private AuthHeaders() {}

    public static final String USER_ID       = "X-User-Id";
    public static final String USERNAME      = "X-User-Name";
    public static final String ROLES         = "X-User-Roles";
    public static final String TOKEN_ID      = "X-Token-Id";
    public static final String TOKEN_VERSION = "X-Token-Version";
    public static final String CORRELATION_ID = "X-Correlation-Id";
}
