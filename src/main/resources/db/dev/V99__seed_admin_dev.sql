DO $$
DECLARE
  tenant_id_val BIGINT;
  admin_role_id BIGINT;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'tenant'
  ) THEN
    RETURN;
  END IF;

  INSERT INTO tenant(key, name, active)
  SELECT 'demo', 'Demo', true
  WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE key = 'demo');

  SELECT id INTO tenant_id_val FROM tenant WHERE key = 'demo';

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'role'
  ) THEN
    INSERT INTO role(name, tenant_id)
    SELECT 'ADMIN', tenant_id_val
    WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ADMIN' AND tenant_id = tenant_id_val);

    SELECT id INTO admin_role_id FROM role WHERE name = 'ADMIN' AND tenant_id = tenant_id_val;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'app_user'
  ) THEN
    INSERT INTO app_user(username, email, password, enabled, tenant_id)
    SELECT 'admin', 'admin@example.com',
           '{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZo5e.P7ZrjvBy06Rj3S2G8V4k0rsgj8eWcGa',
           true, tenant_id_val
    WHERE NOT EXISTS (
      SELECT 1 FROM app_user WHERE username = 'admin' AND tenant_id = tenant_id_val
    );
  END IF;

  IF admin_role_id IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'user_role'
  ) THEN
    INSERT INTO user_role(user_id, role_id)
    SELECT u.id, admin_role_id
    FROM app_user u
    WHERE u.username = 'admin' AND u.tenant_id = tenant_id_val
      AND NOT EXISTS (
        SELECT 1 FROM user_role ur WHERE ur.user_id = u.id AND ur.role_id = admin_role_id
      );
  END IF;
END$$;

