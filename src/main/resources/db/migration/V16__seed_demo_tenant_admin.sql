DO $$
DECLARE
  demo_tenant_id uuid := '00000000-0000-0000-0000-000000000001';
  admin_role_id  uuid := '00000000-0000-0000-0000-000000000002';

  tenant_key_col_type text;
  role_has_tenant_id  boolean;
  user_pwd_col        text;
BEGIN
  SELECT c.data_type
    INTO tenant_key_col_type
    FROM information_schema.columns c
   WHERE c.table_schema = 'public'
     AND c.table_name = 'iam_tenant'
     AND c.column_name = 'key';

  IF NOT EXISTS (SELECT 1 FROM iam_tenant WHERE tenant_key = 'demo') THEN
    IF tenant_key_col_type IS NULL THEN
      EXECUTE
        'insert into iam_tenant (id, tenant_key, name) values ($1, $2, $3)'
      USING demo_tenant_id, 'demo', 'Demo Tenant';
    ELSIF tenant_key_col_type = 'uuid' THEN
      EXECUTE
        'insert into iam_tenant (id, tenant_key, name, "key") values ($1, $2, $3, $4)'
      USING demo_tenant_id, 'demo', 'Demo Tenant', '00000000-0000-0000-0000-00000000aaaa'::uuid;
    ELSIF tenant_key_col_type = 'bytea' THEN
      EXECUTE
        'insert into iam_tenant (id, tenant_key, name, "key") values ($1, $2, $3, $4)'
      USING demo_tenant_id, 'demo', 'Demo Tenant',
            decode('00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff', 'hex');
    ELSE
      EXECUTE
        'insert into iam_tenant (id, tenant_key, name, "key") values ($1, $2, $3, $4)'
      USING demo_tenant_id, 'demo', 'Demo Tenant',
            '00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff';
    END IF;
  END IF;

  SELECT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'iam_role'
       AND column_name = 'tenant_id'
  )
  INTO role_has_tenant_id;

  IF role_has_tenant_id THEN
    IF NOT EXISTS (SELECT 1 FROM iam_role WHERE tenant_id = demo_tenant_id AND name = 'ADMIN') THEN
      EXECUTE
        'insert into iam_role (tenant_id, id, name) values ($1, $2, $3)'
      USING demo_tenant_id, admin_role_id, 'ADMIN';
    END IF;
  ELSE
    IF NOT EXISTS (SELECT 1 FROM iam_role WHERE name = 'ADMIN') THEN
      EXECUTE
        'insert into iam_role (id, name) values ($1, $2)'
      USING admin_role_id, 'ADMIN';
    END IF;
  END IF;

  SELECT CASE
           WHEN EXISTS (
             SELECT 1 FROM information_schema.columns
              WHERE table_schema = 'public'
                AND table_name = 'iam_user'
                AND column_name = 'password_hash'
           ) THEN 'password_hash'
           WHEN EXISTS (
             SELECT 1 FROM information_schema.columns
              WHERE table_schema = 'public'
                AND table_name = 'iam_user'
                AND column_name = 'password'
           ) THEN 'password'
           ELSE NULL
         END
    INTO user_pwd_col;

  IF user_pwd_col IS NULL THEN
    RAISE EXCEPTION 'iam_user has no password column (password_hash or password)';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM iam_user
     WHERE tenant_id = demo_tenant_id
       AND username = 'admin'
  ) THEN
    EXECUTE format(
      'insert into iam_user (tenant_id, username, %I, enabled) values ($1, $2, $3, $4)',
      user_pwd_col
    )
    USING demo_tenant_id,
          'admin',
          '{bcrypt}$2y$10$7dl8zrLL6AFSi9jfYSfxXuufXFVTSZCwh8QFSpgzg8lsm4RnpADBa',
          true;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM iam_user_role
     WHERE tenant_id = demo_tenant_id
       AND username = 'admin'
       AND role_id  = admin_role_id
  ) THEN
    EXECUTE
      'insert into iam_user_role (tenant_id, username, role_id) values ($1, $2, $3)'
    USING demo_tenant_id, 'admin', admin_role_id;
  END IF;
END$$;

