create extension if not exists pgcrypto;

create table if not exists iam_tenant (
  id uuid primary key default gen_random_uuid(),
  tenant_key varchar(64) not null unique,
  name varchar(200) not null,
  created_at timestamptz not null default now()
);

create table if not exists iam_user (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id) on delete cascade,
  username varchar(100) not null,
  password_hash varchar(255) not null,
  enabled boolean not null default true,
  created_at timestamptz not null default now(),
  constraint uk_iam_user_tenant_username unique (tenant_id, username)
);

create index if not exists ix_iam_user_tenant_username on iam_user(tenant_id, username);

create table if not exists iam_role (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id) on delete cascade,
  name varchar(80) not null,
  created_at timestamptz not null default now(),
  constraint uk_iam_role_tenant_name unique (tenant_id, name)
);

create index if not exists ix_iam_role_tenant_name on iam_role(tenant_id, name);

create table if not exists iam_user_role (
  tenant_id uuid not null,
  username varchar(100) not null,
  role_id uuid not null references iam_role(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint pk_iam_user_role primary key (tenant_id, username, role_id),
  constraint fk_iam_user_role_user foreign key (tenant_id, username)
    references iam_user(tenant_id, username) on delete cascade
);

create index if not exists ix_iam_user_role_tenant_username on iam_user_role(tenant_id, username);
create index if not exists ix_iam_user_role_role_id on iam_user_role(role_id);

