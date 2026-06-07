CREATE TABLE user_profiles (
  user_id VARCHAR(64) PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(20),
  first_name VARCHAR(64),
  last_name VARCHAR(64),
  avatar_url VARCHAR(1024),
  date_of_birth DATE,
  gender VARCHAR(16),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_profile_email ON user_profiles(email);

CREATE TABLE user_addresses (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL REFERENCES user_profiles(user_id) ON DELETE CASCADE,
  label VARCHAR(64),
  recipient_name VARCHAR(128) NOT NULL,
  recipient_phone VARCHAR(20) NOT NULL,
  street TEXT NOT NULL,
  ward VARCHAR(128),
  district VARCHAR(128),
  city VARCHAR(128) NOT NULL,
  country VARCHAR(64) NOT NULL DEFAULT 'Vietnam',
  postal_code VARCHAR(16),
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_address_user ON user_addresses(user_id);
CREATE UNIQUE INDEX ux_address_one_default ON user_addresses(user_id) WHERE is_default = TRUE;
