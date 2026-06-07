-- Voucher service schema init

CREATE TABLE IF NOT EXISTS vouchers (
    id                      VARCHAR(36) PRIMARY KEY,
    code                    VARCHAR(50)    NOT NULL,
    type                    VARCHAR(20)    NOT NULL,
    discount_value          NUMERIC(19,2)  NOT NULL,
    min_order_amount        NUMERIC(19,2),
    max_discount_amount     NUMERIC(19,2),
    usage_limit             INTEGER,
    usage_limit_per_user    INTEGER,
    used_count              INTEGER        NOT NULL DEFAULT 0,
    start_date              TIMESTAMPTZ,
    end_date                TIMESTAMPTZ,
    active                  BOOLEAN        NOT NULL DEFAULT TRUE,
    target_user_type        VARCHAR(20)    DEFAULT 'ALL',
    target_user_ids         TEXT,
    applicable_product_ids  TEXT,
    description             TEXT,
    created_at              TIMESTAMPTZ    NOT NULL,
    updated_at              TIMESTAMPTZ,
    version                 BIGINT         DEFAULT 0,

    CONSTRAINT uq_voucher_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_voucher_active   ON vouchers (active);
CREATE INDEX IF NOT EXISTS idx_voucher_end_date ON vouchers (end_date);

-- Per-user usage tracking: one record per successful voucher application
CREATE TABLE IF NOT EXISTS voucher_usage_records (
    id          VARCHAR(36)  PRIMARY KEY,
    voucher_id  VARCHAR(36)       NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    order_id    VARCHAR(255) NOT NULL,
    used_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_usage_order_id UNIQUE (order_id)
);

CREATE INDEX IF NOT EXISTS idx_usage_voucher_user ON voucher_usage_records (voucher_id, user_id);
CREATE INDEX IF NOT EXISTS idx_usage_order        ON voucher_usage_records (order_id);
