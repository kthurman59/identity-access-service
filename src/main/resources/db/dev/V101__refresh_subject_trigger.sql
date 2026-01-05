-- fill subject from username when missing
create or replace function rt_subject_default() returns trigger
language plpgsql as $$
begin
  if new.subject is null or length(new.subject) = 0 then
    new.subject := coalesce(new.username, new.token_hash);
  end if;
  return new;
end $$;

drop trigger if exists trg_rt_subject_default on refresh_token;

create trigger trg_rt_subject_default
before insert or update on refresh_token
for each row execute function rt_subject_default();

