CREATE TABLE flash_sales (
  id             VARCHAR(36)    PRIMARY KEY,
  product_id     VARCHAR(64)    NOT NULL,
  product_name   VARCHAR(255),
  original_price NUMERIC(19,2)  NOT NULL,
  sale_price     NUMERIC(19,2)  NOT NULL CHECK (sale_price < original_price),
  total_slots    INT            NOT NULL CHECK (total_slots > 0),
  reserved_slots INT            NOT NULL DEFAULT 0 CHECK (reserved_slots >= 0 AND reserved_slots <= total_slots),
  max_per_user   INT            NOT NULL DEFAULT 1 CHECK (max_per_user > 0),
  start_time     TIMESTAMPTZ    NOT NULL,
  end_time       TIMESTAMPTZ    NOT NULL,
  status         VARCHAR(16)    NOT NULL,
  description    TEXT,
  created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
  version        BIGINT         NOT NULL DEFAULT 0,
  CHECK (end_time > start_time)
);

CREATE INDEX ix_flashsale_status  ON flash_sales(status);
CREATE INDEX ix_flashsale_start   ON flash_sales(start_time);
CREATE INDEX ix_flashsale_product ON flash_sales(product_id);

CREATE TABLE flash_sale_participations (
  id             VARCHAR(36)  PRIMARY KEY,
  sale_id        VARCHAR(36)  NOT NULL REFERENCES flash_sales(id) ON DELETE CASCADE,
  user_id        VARCHAR(64)  NOT NULL,
  quantity       INT          NOT NULL CHECK (quantity > 0),
  participated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_participation_sale_user ON flash_sale_participations(sale_id, user_id);
