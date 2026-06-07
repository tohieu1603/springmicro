-- Composite indexes backing cursor pagination on the `orders` table.
--
-- Cursor queries always sort `(created_at DESC, id DESC)` and frequently filter
-- on `user_id` or `status` first; without these indexes the planner falls back to
-- a Seq Scan + Sort once the table grows past a few thousand rows.
--
-- Order of columns matters: leading `user_id` / `status` lets the planner satisfy
-- the WHERE clause via index lookup, then the trailing `(created_at, id)` provides
-- the sort order so PostgreSQL can stream results without materializing a sort.

CREATE INDEX IF NOT EXISTS idx_orders_cursor_global
    ON orders (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_orders_cursor_by_status
    ON orders (status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_orders_cursor_by_user
    ON orders (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_orders_cursor_by_user_status
    ON orders (user_id, status, created_at DESC, id DESC);
