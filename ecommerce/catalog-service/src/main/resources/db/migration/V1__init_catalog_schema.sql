-- V1 — Catalog schema.
-- Tables: categories, attrs, attr_vals, products, variants, variant_attrs
-- Ids are VARCHAR(36) UUID strings, assigned by the domain at aggregate creation.

CREATE TABLE categories (
    id           VARCHAR(36) PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    parent_id    VARCHAR(36) REFERENCES categories(id) ON DELETE SET NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(64),
    updated_by   VARCHAR(64)
);
CREATE UNIQUE INDEX ux_categories_name_lower ON categories (LOWER(name));
CREATE INDEX ix_categories_parent ON categories (parent_id);
CREATE INDEX ix_categories_active_sort ON categories (active, sort_order);

CREATE TABLE attrs (
    id          VARCHAR(36) PRIMARY KEY,
    code        VARCHAR(64) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(16) NOT NULL,  -- SELECT | TEXT | NUMBER
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_attrs_code ON attrs (code);

CREATE TABLE attr_vals (
    id          VARCHAR(36) PRIMARY KEY,
    attr_id     VARCHAR(36) NOT NULL REFERENCES attrs(id) ON DELETE CASCADE,
    val         VARCHAR(100) NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attr_vals_per_attr UNIQUE (attr_id, code)
);
CREATE INDEX ix_attr_vals_attr ON attr_vals (attr_id);

CREATE TABLE products (
    id                VARCHAR(36) PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(128) NOT NULL,
    description       TEXT,
    category_id       VARCHAR(36) REFERENCES categories(id) ON DELETE SET NULL,
    brand             VARCHAR(100),
    thumbnail         VARCHAR(1024),
    images            TEXT,                     -- JSON array serialised as text
    status            VARCHAR(16) NOT NULL,     -- DRAFT | ACTIVE | INACTIVE | DELETED
    meta_title        VARCHAR(255),
    meta_description  VARCHAR(500),
    meta_keywords     VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(64),
    updated_by        VARCHAR(64),
    version           BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_products_slug ON products (slug);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_status ON products (status);
-- Keyset pagination order: (created_at DESC, id DESC).
CREATE INDEX ix_products_list ON products (created_at DESC, id DESC);

CREATE TABLE variants (
    id           VARCHAR(36) PRIMARY KEY,
    product_id   VARCHAR(36) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku          VARCHAR(64) NOT NULL,
    price        NUMERIC(19,2) NOT NULL,
    cost         NUMERIC(19,2),
    sale_price   NUMERIC(19,2),
    image        VARCHAR(1024),
    weight       NUMERIC(10,3),
    quantity     INT NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL, -- ACTIVE | INACTIVE | OUT_OF_STOCK
    version      BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_variants_sku ON variants (sku);
CREATE INDEX ix_variants_product ON variants (product_id);

CREATE TABLE variant_attrs (
    id          VARCHAR(36) PRIMARY KEY,
    variant_id  VARCHAR(36) NOT NULL REFERENCES variants(id) ON DELETE CASCADE,
    attr_id     VARCHAR(36) NOT NULL REFERENCES attrs(id) ON DELETE RESTRICT,
    attr_code   VARCHAR(64)  NOT NULL,  -- denormalised snapshot
    attr_name   VARCHAR(100) NOT NULL,  -- denormalised snapshot
    val_id      VARCHAR(36) REFERENCES attr_vals(id) ON DELETE RESTRICT,
    val_text    VARCHAR(255),
    CONSTRAINT ck_variant_attrs_value CHECK (val_id IS NOT NULL OR val_text IS NOT NULL),
    CONSTRAINT uq_variant_attrs_per_variant UNIQUE (variant_id, attr_id)
);
CREATE INDEX ix_variant_attrs_variant ON variant_attrs (variant_id);
