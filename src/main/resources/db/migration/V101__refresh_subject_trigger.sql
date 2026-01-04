ALTER TABLE refresh_token
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_refresh_token_updated_at'
  ) THEN
    CREATE TRIGGER trg_refresh_token_updated_at
      BEFORE UPDATE ON refresh_token
      FOR EACH ROW
      EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

