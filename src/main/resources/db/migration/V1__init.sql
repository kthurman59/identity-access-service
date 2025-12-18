create extension if not exists pgcrypto;

create table iam_tenant (
  id uuid primary key default gen_random_uuid(),
  slug varchar(255) not null unique,
  name varchar(255) not null,
  status varchar(16) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uq_iam_tenant_slug unique (slug)
);

create table iam_user (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  username varchar(64) not null,
  email varchar(255) not null,
  password_hash varchar(255) not null,
  enabled boolean not null default true,
  locked boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_iam_user_tenant foreign key (tenant_id) references iam_tenant(id) on delete cascade,
  constraint uq_iam_user_tenant_username unique (tenant_id, username),
  constraint uq_iam_user_tenant_email unique (tenant_id, email)
);

create table iam_role (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  name varchar(64) not null,
  description varchar(255),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_iam_role_tenant foreign key (tenant_id) references iam_tenant(id) on delete cascade,
  constraint uq_iam_role_tenant_name unique (tenant_id, name)
);

create table iam_user_role (
  tenant_id uuid not null,
  user_id uuid not null,
  role_id uuid not null,
  created_at timestamptz not null default now(),
  constraint pk_iam_user_role primary key (user_id, role_id),
  constraint fk_iam_user_role_tenant foreign key (tenant_id) references iam_tenant(id) on delete cascade,
  constraint fk_iam_user_role_user foreign key (user_id) references iam_user(id) on delete cascade,
  constraint fk_iam_user_role_role foreign key (role_id) references iam_role(id) on delete cascade
);

create index idx_iam_user_tenant_id on iam_user(tenant_id);
create index idx_iam_role_tenant_id on iam_role(tenant_id);
create index idx_iam_user_role_tenant_id on iam_user_role(tenant_id);
create index idx_iam_user_role_role_id on iam_user_role(role_id);

