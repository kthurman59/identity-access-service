create or replace function set_refresh_subject() returns trigger as $$
begin
  if NEW.subject is null or NEW.subject = '' then
    NEW.subject := NEW.username;
  end if;
  return NEW;
end;
$$ language plpgsql;

drop trigger if exists trg_set_refresh_subject on refresh_token;

create trigger trg_set_refresh_subject
before insert on refresh_token
for each row execute function set_refresh_subject();
