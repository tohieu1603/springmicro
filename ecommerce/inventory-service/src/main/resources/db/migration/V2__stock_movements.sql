-- Audit trail for every stock-changing operation (adjust, reserve, confirm, release).
-- We don't normalise productId / sku into FKs because inventories.id can be deleted
-- but we still want to keep the history. Keeps the table append-only.

CREATE TABLE stock_movements (
  id           VARCHAR(36)  PRIMARY KEY,
  product_id   VARCHAR(36)  NOT NULL,
  sku          VARCHAR(64)  NOT NULL,
  delta        INT          NOT NULL,
  quantity_before INT       NOT NULL,
  quantity_after  INT       NOT NULL,
  reserved_after  INT       NOT NULL DEFAULT 0,
  reason       VARCHAR(32)  NOT NULL,         -- ADJUST | RESERVE | CONFIRM | RELEASE | CREATE
  reference_id VARCHAR(64),                   -- orderId for reservations, null for adjustments
  actor        VARCHAR(64),                   -- userId or 'SYSTEM'
  note         TEXT,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_movements_product   ON stock_movements(product_id, created_at DESC);
CREATE INDEX ix_movements_sku       ON stock_movements(sku, created_at DESC);
CREATE INDEX ix_movements_reason    ON stock_movements(reason);
CREATE INDEX ix_movements_reference ON stock_movements(reference_id);
