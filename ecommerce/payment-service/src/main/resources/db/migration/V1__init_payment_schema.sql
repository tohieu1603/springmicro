-- V1 — Payment schema.

CREATE TABLE payments (
    id               VARCHAR(36)      PRIMARY KEY,
    order_id         VARCHAR(64)      NOT NULL,
    user_id          VARCHAR(64)      NOT NULL,
    amount           NUMERIC(19,2)    NOT NULL,
    currency         VARCHAR(8)       NOT NULL DEFAULT 'VND',
    method           VARCHAR(32)      NOT NULL,
    status           VARCHAR(32)      NOT NULL,
    transaction_id   VARCHAR(128),
    gateway_response TEXT,
    qr_code_url      VARCHAR(1024),
    pay_url          VARCHAR(1024),
    paid_at          TIMESTAMPTZ,
    refund_amount    NUMERIC(19,2),
    notes            TEXT,
    created_at       TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ      NOT NULL DEFAULT now(),
    version          BIGINT           NOT NULL DEFAULT 0,
    idempotency_key  VARCHAR(128),
    CONSTRAINT uk_payments_order_id       UNIQUE (order_id),
    CONSTRAINT uk_payments_idempotency    UNIQUE (idempotency_key)
);

CREATE INDEX ix_payments_user_id  ON payments (user_id);
CREATE INDEX ix_payments_order_id ON payments (order_id);
CREATE INDEX ix_payments_status   ON payments (status);
