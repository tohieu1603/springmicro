CREATE TABLE inventories (
  id VARCHAR(36) PRIMARY KEY,
  product_id VARCHAR(36) NOT NULL UNIQUE,
  sku VARCHAR(64) NOT NULL UNIQUE,
  quantity INT NOT NULL CHECK (quantity >= 0),
  reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
  min_stock_level INT NOT NULL DEFAULT 10,
  last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (reserved_quantity <= quantity)
);
CREATE INDEX ix_inventories_product ON inventories(product_id);
CREATE INDEX ix_inventories_sku ON inventories(sku);

CREATE TABLE stock_reservations (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL UNIQUE,
  items TEXT NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_reservations_order ON stock_reservations(order_id);
CREATE INDEX ix_reservations_status ON stock_reservations(status);
