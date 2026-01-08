-- Add updated_at to refresh_token to match JPA mapping

alter table if exists refresh_token
  add column if not exists updated_at timestamp with time zone;

update refresh_token
set updated_at = coalesce(updated_at, created_at, now())
where updated_at is null;

alter table refresh_token
  alter column updated_at set default now();

alter table refresh_token
  alter column updated_at set not null;

create or replace function refresh_token_set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists trg_refresh_token_set_updated_at on refresh_token;

create trigger trg_refresh_token_set_updated_at
before update on refresh_token
for each row
execute function refresh_token_set_updated_at();

