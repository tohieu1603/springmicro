-- =====================================================================
-- V2: Add google_sub column to support "Login with Google".
--
-- Lookup strategy: google_sub → email. Stored as a nullable VARCHAR
-- (existing password accounts have no Google identity). Unique to prevent
-- two distinct HIEU users from claiming the same Google account.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_sub
    ON users (google_sub)
    WHERE google_sub IS NOT NULL;
