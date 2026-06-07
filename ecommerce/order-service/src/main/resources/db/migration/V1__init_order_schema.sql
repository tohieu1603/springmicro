-- Order Service: initial schema (UUID string PKs)

CREATE TABLE IF NOT EXISTS orders (
    id               VARCHAR(36)     NOT NULL PRIMARY KEY,
    order_number     VARCHAR(30)     NOT NULL,
    user_id          VARCHAR(255)    NOT NULL,
    status           VARCHAR(30)     NOT NULL,
    subtotal_amount  NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    shipping_fee     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_amount     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    voucher_code     VARCHAR(50),
    recipient_name   VARCHAR(255)    NOT NULL,
    recipient_phone  VARCHAR(30)     NOT NULL,
    street           VARCHAR(500)    NOT NULL,
    ward             VARCHAR(100)    NOT NULL,
    district         VARCHAR(100)    NOT NULL,
    city             VARCHAR(100)    NOT NULL,
    country          VARCHAR(100)    NOT NULL DEFAULT 'Vietnam',
    postal_code      VARCHAR(20),
    notes            VARCHAR(1000),
    payment_method   VARCHAR(30)     NOT NULL,
    payment_id       VARCHAR(36),
    reservation_id   VARCHAR(128),
    shipment_id      VARCHAR(36),
    idempotency_key  VARCHAR(128),
    failure_reason   TEXT,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    delivered_at     TIMESTAMPTZ,
    cancelled_at     TIMESTAMPTZ,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_order_number   UNIQUE (order_number),
    CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS order_items (
    id           VARCHAR(36)     NOT NULL PRIMARY KEY,
    order_id     VARCHAR(36)     NOT NULL,
    product_id   VARCHAR(36)     NOT NULL,
    product_name VARCHAR(500)    NOT NULL,
    variant_id   VARCHAR(36),
    variant_sku  VARCHAR(100),
    variant_image VARCHAR(1000),
    unit_price   NUMERIC(19, 2)  NOT NULL,
    quantity     INTEGER         NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS return_requests (
    id            VARCHAR(36)     NOT NULL PRIMARY KEY,
    order_id      VARCHAR(36)     NOT NULL,
    user_id       VARCHAR(255)    NOT NULL,
    reason        VARCHAR(1000)   NOT NULL,
    return_type   VARCHAR(20)     NOT NULL,
    status        VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    refund_amount NUMERIC(19, 2),
    admin_note    TEXT,
    images        TEXT,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key      VARCHAR(128)    NOT NULL PRIMARY KEY,
    order_id             VARCHAR(36),
    status               VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING',
    response_body        TEXT,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at           TIMESTAMPTZ     NOT NULL,
    processing_started_at TIMESTAMPTZ
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id      ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at   ON orders(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_return_requests_order_id ON return_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_user_id  ON return_requests(user_id);
