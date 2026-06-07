-- V1: cart_items table
CREATE TABLE IF NOT EXISTS cart_items (
    id           VARCHAR(36)     PRIMARY KEY,
    user_id      VARCHAR(64)     NOT NULL,
    product_id   VARCHAR(36)     NOT NULL,
    product_name VARCHAR(255)    NOT NULL,
    variant_id   VARCHAR(36)     NOT NULL,
    variant_sku  VARCHAR(64)     NOT NULL,
    variant_image VARCHAR(1024),
    unit_price   NUMERIC(19, 2)  NOT NULL,
    quantity     INT             NOT NULL CHECK (quantity BETWEEN 1 AND 999),
    version      BIGINT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_user_variant UNIQUE (user_id, variant_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_user_id ON cart_items (user_id);
