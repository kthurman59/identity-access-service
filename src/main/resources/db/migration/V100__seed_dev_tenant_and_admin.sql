-- V100__seed_dev_tenant_and_admin.sql
DO $$
DECLARE
  t_id uuid;
  u_id uuid;
  pwdcol text;
BEGIN
  -- upsert tenant 'dev'
  SELECT id INTO t_id FROM iam_tenant WHERE key = 'dev';
  IF t_id IS NULL THEN
    INSERT INTO iam_tenant(slug, name, key) VALUES ('dev', 'Dev', 'dev')
    RETURNING id INTO t_id;
  END IF;

  -- detect the password column on iam_user
  SELECT a.attname INTO pwdcol
  FROM pg_attribute a
  JOIN pg_class c ON c.oid = a.attrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public'
    AND c.relname = 'iam_user'
    AND a.attnum > 0
    AND NOT a.attisdropped
    AND a.attname IN ('password','password_hash','password_encoded','encoded_password','passwd','pwd_hash')
  ORDER BY CASE a.attname
             WHEN 'password' THEN 1
             WHEN 'password_hash' THEN 2
             WHEN 'password_encoded' THEN 3
             WHEN 'encoded_password' THEN 4
             WHEN 'passwd' THEN 5
             WHEN 'pwd_hash' THEN 6
             ELSE 99
           END
  LIMIT 1;

  IF pwdcol IS NULL THEN
    RAISE EXCEPTION 'No password-like column on iam_user';
  END IF;

  -- upsert admin user in tenant 'dev'
  IF NOT EXISTS (SELECT 1 FROM iam_user WHERE username = 'admin' AND tenant_id = t_id) THEN
    EXECUTE format(
      'INSERT INTO iam_user(username, email, %I, enabled, tenant_id)
       VALUES ($1, $2, $3, $4, $5) RETURNING id', pwdcol
    )
    USING 'admin', 'admin@example.com', '{noop}Admin123!', true, t_id
    INTO u_id;
  ELSE
    SELECT id INTO u_id FROM iam_user WHERE username = 'admin' AND tenant_id = t_id;
  END IF;

  -- upsert ADMIN role for that tenant
  IF NOT EXISTS (SELECT 1 FROM iam_role WHERE name = 'ADMIN' AND tenant_id = t_id) THEN
    INSERT INTO iam_role(name, tenant_id) VALUES ('ADMIN', t_id);
  END IF;

  -- link admin to ADMIN role
  IF NOT EXISTS (
    SELECT 1
    FROM iam_user_role ur
    WHERE ur.user_id = u_id
      AND ur.role_id = (SELECT id FROM iam_role WHERE name = 'ADMIN' AND tenant_id = t_id)
      AND ur.tenant_id = t_id
  ) THEN
    INSERT INTO iam_user_role(user_id, role_id, tenant_id)
    SELECT u_id, id, t_id FROM iam_role WHERE name = 'ADMIN' AND tenant_id = t_id;
  END IF;
END $$;

