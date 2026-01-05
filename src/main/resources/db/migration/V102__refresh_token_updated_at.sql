-- ensure updated_at exists and is maintained
alter table refresh_token
  add column if not exists updated_at timestamptz not null default now();

create or replace function set_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

drop trigger if exists trg_rt_updated_at on refresh_token;

create trigger trg_rt_updated_at
before update on refresh_token
for each row execute function set_updated_at();

