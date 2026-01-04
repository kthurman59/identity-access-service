ALTER TABLE refresh_token
  ADD COLUMN IF NOT EXISTS updated_at timestamptz;

UPDATE refresh_token
SET updated_at = COALESCE(updated_at, created_at)
WHERE updated_at IS NULL;

CREATE OR REPLACE FUNCTION refresh_token_touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_refresh_token_touch_updated_at ON refresh_token;

CREATE TRIGGER trg_refresh_token_touch_updated_at
BEFORE UPDATE ON refresh_token
FOR EACH ROW
EXECUTE FUNCTION refresh_token_touch_updated_at();

