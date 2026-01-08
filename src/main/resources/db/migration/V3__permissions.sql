create table if not exists iam_permission (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id) on delete cascade,
  code varchar(120) not null,
  description varchar(255),
  created_at timestamptz not null default now(),
  constraint uk_iam_permission_tenant_code unique (tenant_id, code)
);

create index if not exists ix_iam_permission_tenant_code on iam_permission(tenant_id, code);

create table if not exists iam_role_permission (
  tenant_id uuid not null references iam_tenant(id) on delete cascade,
  role_id uuid not null references iam_role(id) on delete cascade,
  permission_id uuid not null references iam_permission(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint pk_iam_role_permission primary key (tenant_id, role_id, permission_id)
);

create index if not exists ix_iam_role_permission_role on iam_role_permission(role_id);
create index if not exists ix_iam_role_permission_perm on iam_role_permission(permission_id);

