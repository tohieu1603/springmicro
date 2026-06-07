CREATE TABLE outbox_events (
  id            VARCHAR(36)  NOT NULL PRIMARY KEY DEFAULT gen_random_uuid()::text,
  aggregate_type VARCHAR(64)  NOT NULL,
  aggregate_id  VARCHAR(64)  NOT NULL,
  event_type    VARCHAR(128) NOT NULL,
  topic         VARCHAR(128) NOT NULL,
  payload       JSONB NOT NULL,
  headers       JSONB,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  processed_at  TIMESTAMPTZ,
  retry_count   INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_outbox_pending ON outbox_events(processed_at, next_attempt_at) WHERE processed_at IS NULL;
CREATE INDEX ix_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
