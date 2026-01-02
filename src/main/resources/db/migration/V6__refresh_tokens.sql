create table refresh_token (
  id uuid primary key,
  subject varchar(100) not null,
  token_hash text not null,
  issued_at timestamptz not null default now(),
  expires_at timestamptz not null,
  revoked_at timestamptz,
  replaced_by uuid,
  constraint fk_refresh_token_replaced_by foreign key (replaced_by) references refresh_token(id)
);

create index idx_refresh_token_subject on refresh_token(subject);
create index idx_refresh_token_token_hash on refresh_token(token_hash);

