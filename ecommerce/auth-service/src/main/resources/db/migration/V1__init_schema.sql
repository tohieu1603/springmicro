-- =====================================================================
-- Auth-service initial schema.
--
-- Naming conventions:
--   * tables      : snake_case plural (users, roles)
--   * FK columns  : {table_singular}_id
--   * indexes     : idx_{table}_{column(s)}
--
-- All primary keys are UUID strings (36 chars) assigned by the domain at
-- aggregate creation — no DB sequences to avoid round-trips.
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id                       VARCHAR(36) PRIMARY KEY,
    username                 VARCHAR(50)  NOT NULL UNIQUE,
    email                    VARCHAR(100) NOT NULL UNIQUE,
    password                 VARCHAR(255) NOT NULL,
    first_name               VARCHAR(64)  NOT NULL,
    last_name                VARCHAR(64)  NOT NULL,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired      BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked       BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired  BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login               TIMESTAMP WITH TIME ZONE,
    token_version            INTEGER      NOT NULL DEFAULT 1,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS permissions (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    resource    VARCHAR(50),
    action      VARCHAR(50),
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ── Many-to-many join tables ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       VARCHAR(36) NOT NULL REFERENCES roles(id)       ON DELETE CASCADE,
    permission_id VARCHAR(36) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ── Tokens ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id             VARCHAR(36)  PRIMARY KEY,
    token          VARCHAR(500) NOT NULL UNIQUE,
    user_id        VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date    TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked        BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at     TIMESTAMP WITH TIME ZONE,
    family         VARCHAR(100) NOT NULL,
    generation     INTEGER      NOT NULL DEFAULT 0,
    revoked_reason VARCHAR(50),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rt_user_id         ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_rt_family          ON refresh_tokens(family);
CREATE INDEX IF NOT EXISTS idx_rt_revoked_expiry  ON refresh_tokens(revoked, expiry_date);

-- Access-token blacklist. Primary key is the JWT jti; rows purged hourly.
CREATE TABLE IF NOT EXISTS token_revocations (
    id          VARCHAR(36)  PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    reason      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_token_revocations_user_id    ON token_revocations(user_id);
CREATE INDEX IF NOT EXISTS idx_token_revocations_expires_at ON token_revocations(expires_at);
