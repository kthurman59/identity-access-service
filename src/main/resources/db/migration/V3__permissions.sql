create table iam_permission (
  id uuid primary key default gen_random_uuid(),
  code varchar(64) not null,
  description varchar(255),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uq_iam_permission_code unique (code)
);

create table iam_role_permission (
  role_id uuid not null,
  permission_id uuid not null,
  created_at timestamptz not null default now(),
  constraint pk_iam_role_permission primary key (role_id, permission_id),
  constraint fk_iam_role_permission_role foreign key (role_id) references iam_role(id) on delete cascade,
  constraint fk_iam_role_permission_permission foreign key (permission_id) references iam_permission(id) on delete cascade
);

create index idx_iam_role_permission_permission_id on iam_role_permission(permission_id);

create trigger trg_iam_permission_set_updated_at
before update on iam_permission
for each row
execute function iam_set_updated_at();

