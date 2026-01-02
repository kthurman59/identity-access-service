BEGIN;

ALTER TABLE refresh_token
  ADD COLUMN IF NOT EXISTS tenant_id uuid,
  ADD COLUMN IF NOT EXISTS username varchar(100),
  ADD COLUMN IF NOT EXISTS token_hash char(64),
  ADD COLUMN IF NOT EXISTS family_id uuid,
  ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS expires_at timestamptz,
  ADD COLUMN IF NOT EXISTS revoked_at timestamptz,
  ADD COLUMN IF NOT EXISTS replaced_by_hash char(64),
  ADD COLUMN IF NOT EXISTS revoke_reason varchar(50);

CREATE UNIQUE INDEX IF NOT EXISTS uq_refresh_token_hash
  ON refresh_token(token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at
  ON refresh_token(expires_at);

CREATE INDEX IF NOT EXISTS idx_refresh_token_tenant_username
  ON refresh_token(tenant_id, username);

COMMIT;

