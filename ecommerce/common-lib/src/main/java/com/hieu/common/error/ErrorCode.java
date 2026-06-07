package com.hieu.common.error;

import org.springframework.http.HttpStatus;

/**
 * Canonical error codes exposed across services.
 *
 * <p>Stable machine-readable identifiers that clients can branch on without parsing
 * human-readable messages. Naming convention: {@code <DOMAIN>-<NNNN>}; once assigned,
 * codes are never renamed — only added. HTTP status mapping is baked in so services
 * can translate consistently at the web boundary.
 *
 * <p>Domain ranges: COMMON-{4xx,5xx}, AUTH-1xxx, CATALOG-2xxx, ORDER-3xxx,
 * VOUCHER-4xxx, CART-5xxx, INVENTORY-6xxx, SHIPPING-7xxx, APP-{4xx,5xx} (cross-cutting
 * fallbacks emitted by GlobalExceptionHandler when no domain-specific code applies).
 */
public enum ErrorCode {

    // ── Generic (COMMON) ────────────────────────────────────────────────
    VALIDATION_FAILED      ("COMMON-400", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED           ("COMMON-401", HttpStatus.UNAUTHORIZED),
    FORBIDDEN              ("COMMON-403", HttpStatus.FORBIDDEN),
    NOT_FOUND              ("COMMON-404", HttpStatus.NOT_FOUND),
    CONFLICT               ("COMMON-409", HttpStatus.CONFLICT),
    INTERNAL_ERROR         ("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE    ("COMMON-503", HttpStatus.SERVICE_UNAVAILABLE),

    // ── App-level fallbacks emitted by GlobalExceptionHandler ──────────
    APP_BAD_REQUEST        ("APP-400", HttpStatus.BAD_REQUEST),
    APP_UNAUTHORIZED       ("APP-401", HttpStatus.UNAUTHORIZED),
    APP_FORBIDDEN          ("APP-403", HttpStatus.FORBIDDEN),
    APP_CONFLICT           ("APP-409", HttpStatus.CONFLICT),
    APP_UNPROCESSABLE      ("APP-422", HttpStatus.UNPROCESSABLE_ENTITY),
    APP_INTERNAL           ("APP-500", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Auth domain (AUTH-1xxx) ─────────────────────────────────────────
    INVALID_CREDENTIALS    ("AUTH-1001", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_EXISTS    ("AUTH-1002", HttpStatus.CONFLICT),
    USER_NOT_FOUND         ("AUTH-1003", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED          ("AUTH-1004", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID          ("AUTH-1005", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED         ("AUTH-1006", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED       ("AUTH-1007", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID  ("AUTH-1008", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND         ("AUTH-1009", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND   ("AUTH-1010", HttpStatus.NOT_FOUND),
    TOKEN_REVOKED          ("AUTH-1011", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSE_DETECTED   ("AUTH-1012", HttpStatus.UNAUTHORIZED),
    TOKEN_OWNERSHIP_FAIL   ("AUTH-1013", HttpStatus.FORBIDDEN),
    AUTH_RATE_LIMITED      ("AUTH-1014", HttpStatus.TOO_MANY_REQUESTS),
    AUTH_BLACKLISTED       ("AUTH-1015", HttpStatus.UNAUTHORIZED),
    ACCOUNT_EXPIRED        ("AUTH-1016", HttpStatus.FORBIDDEN),
    CREDENTIALS_EXPIRED    ("AUTH-1017", HttpStatus.UNAUTHORIZED),
    OAUTH_TOKEN_INVALID    ("AUTH-1018", HttpStatus.UNAUTHORIZED),
    OAUTH_EMAIL_UNVERIFIED ("AUTH-1019", HttpStatus.FORBIDDEN),
    OAUTH_ACCOUNT_LINKED   ("AUTH-1020", HttpStatus.CONFLICT),

    // ── Catalog domain (CATALOG-2xxx) ──────────────────────────────────
    PRODUCT_NOT_FOUND          ("CATALOG-2001", HttpStatus.NOT_FOUND),
    PRODUCT_ALREADY_EXISTS     ("CATALOG-2002", HttpStatus.CONFLICT),
    PRODUCT_INVALID_STATE      ("CATALOG-2003", HttpStatus.UNPROCESSABLE_ENTITY),
    VARIANT_NOT_FOUND          ("CATALOG-2101", HttpStatus.NOT_FOUND),
    VARIANT_SKU_ALREADY_EXISTS ("CATALOG-2102", HttpStatus.CONFLICT),
    VARIANT_INVALID            ("CATALOG-2103", HttpStatus.UNPROCESSABLE_ENTITY),
    CATEGORY_NOT_FOUND         ("CATALOG-2201", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS    ("CATALOG-2202", HttpStatus.CONFLICT),
    CATEGORY_CYCLE             ("CATALOG-2203", HttpStatus.UNPROCESSABLE_ENTITY),
    ATTR_NOT_FOUND             ("CATALOG-2301", HttpStatus.NOT_FOUND),
    ATTR_ALREADY_EXISTS        ("CATALOG-2302", HttpStatus.CONFLICT),
    ATTR_VAL_NOT_FOUND         ("CATALOG-2303", HttpStatus.NOT_FOUND),
    ATTR_INVALID_TYPE          ("CATALOG-2304", HttpStatus.UNPROCESSABLE_ENTITY),

    // ── Order domain (ORDER-3xxx) ──────────────────────────────────────
    ORDER_NOT_FOUND            ("ORDER-3001", HttpStatus.NOT_FOUND),
    ORDER_ALREADY_EXISTS       ("ORDER-3002", HttpStatus.CONFLICT),
    ORDER_INVALID_STATE        ("ORDER-3003", HttpStatus.UNPROCESSABLE_ENTITY),
    ORDER_INSUFFICIENT_STOCK   ("ORDER-3004", HttpStatus.CONFLICT),
    ORDER_DUPLICATE            ("ORDER-3005", HttpStatus.CONFLICT),
    ORDER_SERVICE_UNAVAILABLE  ("ORDER-3006", HttpStatus.SERVICE_UNAVAILABLE),
    ORDER_CANCELLED            ("ORDER-3007", HttpStatus.CONFLICT),
    ORDER_EMPTY_CART           ("ORDER-3008", HttpStatus.UNPROCESSABLE_ENTITY),
    RETURN_REQUEST_NOT_FOUND   ("ORDER-3101", HttpStatus.NOT_FOUND),
    RETURN_REQUEST_INVALID     ("ORDER-3102", HttpStatus.UNPROCESSABLE_ENTITY),

    // ── Voucher domain (VOUCHER-4xxx) ──────────────────────────────────
    VOUCHER_NOT_FOUND          ("VOUCHER-4004", HttpStatus.NOT_FOUND),
    VOUCHER_LIMIT_REACHED      ("VOUCHER-4009", HttpStatus.CONFLICT),
    VOUCHER_REJECTED           ("VOUCHER-4022", HttpStatus.UNPROCESSABLE_ENTITY),

    // ── Cart domain (CART-5xxx) ────────────────────────────────────────
    CART_VALIDATION            ("CART-5400", HttpStatus.BAD_REQUEST),
    CART_UNAUTHORIZED          ("CART-5401", HttpStatus.UNAUTHORIZED),
    CART_FORBIDDEN             ("CART-5403", HttpStatus.FORBIDDEN),
    CART_NOT_FOUND             ("CART-5404", HttpStatus.NOT_FOUND),
    CART_CONFLICT              ("CART-5409", HttpStatus.CONFLICT),
    CART_INTERNAL              ("CART-5500", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Inventory domain (INVENTORY-6xxx) ──────────────────────────────
    INVENTORY_NOT_FOUND        ("INVENTORY-6004", HttpStatus.NOT_FOUND),
    INVENTORY_INSUFFICIENT     ("INVENTORY-6009", HttpStatus.CONFLICT),

    // ── Shipping domain (SHIPPING-7xxx) ────────────────────────────────
    SHIPPING_FORBIDDEN         ("SHIPPING-7403", HttpStatus.FORBIDDEN),
    SHIPMENT_NOT_FOUND         ("SHIPPING-7404", HttpStatus.NOT_FOUND),
    SHIPMENT_DUPLICATE         ("SHIPPING-7409", HttpStatus.CONFLICT),
    SHIPMENT_INVALID_STATE     ("SHIPPING-7422", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code()          { return code; }
    public HttpStatus httpStatus() { return httpStatus; }
}
