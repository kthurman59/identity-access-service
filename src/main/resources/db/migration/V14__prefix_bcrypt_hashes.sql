DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'app_user'
  ) THEN
    UPDATE app_user
    SET password =
      CASE
        WHEN password LIKE '{bcrypt}%' THEN password
        WHEN password ~ '^\$2[aby]\$' THEN '{bcrypt}' || password
        ELSE password
      END;
  END IF;
END$$;

