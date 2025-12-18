create or replace function iam_user_role_set_tenant_and_validate()
returns trigger as $$
declare
  user_tenant uuid;
  role_tenant uuid;
begin
  select tenant_id into user_tenant from iam_user where id = new.user_id;
  select tenant_id into role_tenant from iam_role where id = new.role_id;

  if user_tenant is null then
    raise exception 'iam_user_role user_id not found: %', new.user_id;
  end if;

  if role_tenant is null then
    raise exception 'iam_user_role role_id not found: %', new.role_id;
  end if;

  if user_tenant <> role_tenant then
    raise exception 'iam_user_role tenant mismatch user tenant % role tenant %', user_tenant, role_tenant;
  end if;

  new.tenant_id = user_tenant;
  return new;
end;
$$ language plpgsql;

create trigger trg_iam_user_role_set_tenant
before insert on iam_user_role
for each row
execute function iam_user_role_set_tenant_and_validate();

