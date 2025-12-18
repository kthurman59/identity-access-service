create or replace function iam_set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create trigger trg_iam_tenant_set_updated_at
before update on iam_tenant
for each row
execute function iam_set_updated_at();

create trigger trg_iam_user_set_updated_at
before update on iam_user
for each row
execute function iam_set_updated_at();

create trigger trg_iam_role_set_updated_at
before update on iam_role
for each row
execute function iam_set_updated_at();

