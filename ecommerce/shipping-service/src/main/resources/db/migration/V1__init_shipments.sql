CREATE TABLE IF NOT EXISTS shipments (
    id                      VARCHAR(36)  PRIMARY KEY,
    order_id                VARCHAR(64)  NOT NULL,
    user_id                 VARCHAR(64)  NOT NULL,
    carrier                 VARCHAR(32),
    tracking_number         VARCHAR(64),
    status                  VARCHAR(32)  NOT NULL,
    recipient_name          VARCHAR(128) NOT NULL,
    recipient_phone         VARCHAR(20)  NOT NULL,
    address_line            TEXT         NOT NULL,
    ward                    VARCHAR(128),
    district                VARCHAR(128),
    city                    VARCHAR(128) NOT NULL,
    country                 VARCHAR(64)  NOT NULL DEFAULT 'Vietnam',
    estimated_delivery_date TIMESTAMPTZ,
    actual_delivery_date    TIMESTAMPTZ,
    notes                   TEXT,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_shipment_order_id       UNIQUE (order_id),
    CONSTRAINT uq_shipment_tracking_number UNIQUE (tracking_number)
);

CREATE INDEX IF NOT EXISTS idx_shipment_user_id        ON shipments (user_id);
CREATE INDEX IF NOT EXISTS idx_shipment_status         ON shipments (status);
CREATE INDEX IF NOT EXISTS idx_shipment_tracking_number ON shipments (tracking_number);
CREATE INDEX IF NOT EXISTS idx_shipment_order_id       ON shipments (order_id);
