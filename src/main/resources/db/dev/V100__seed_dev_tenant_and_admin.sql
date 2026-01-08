do $$
declare
  t_id uuid;
  admin_role_id uuid;
begin
  insert into iam_tenant (tenant_key, name, "key")
  values ('dev', 'Development', 'dev')
  on conflict (tenant_key) do update
    set name = excluded.name,
        "key" = excluded."key";

  select id into t_id
  from iam_tenant
  where tenant_key = 'dev';

  if t_id is null then
    raise exception 'dev tenant missing after seed';
  end if;

  insert into iam_user (tenant_id, username, password_hash, enabled)
  values (t_id, 'admin', '$2a$10$REPLACE_WITH_YOUR_BCRYPT_HASH', true)
  on conflict (tenant_id, username) do nothing;

  insert into iam_role (tenant_id, name)
  values (t_id, 'ADMIN')
  on conflict (tenant_id, name) do nothing;

  select id into admin_role_id
  from iam_role
  where tenant_id = t_id and name = 'ADMIN';

  if admin_role_id is null then
    raise exception 'ADMIN role missing after seed';
  end if;

  insert into iam_user_role (tenant_id, username, role_id)
  values (t_id, 'admin', admin_role_id)
  on conflict do nothing;
end $$;


