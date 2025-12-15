create extension if not exists pgcrypto;

create table iam_tenant (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  status text not null default 'ACTIVE',
  created_at timestamptz not null default now()
);

create table iam_user (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id),
  email text not null,
  email_normalized text not null,
  password_hash text not null,
  display_name text,
  status text not null default 'ACTIVE',
  must_change_password boolean not null default false,
  failed_login_count int not null default 0,
  locked_until timestamptz,
  last_login_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tenant_id, email_normalized)
);

create index idx_iam_user_tenant_email on iam_user(tenant_id, email_normalized);

create table iam_role (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id),
  name text not null,
  description text,
  is_system boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tenant_id, name)
);

create table iam_permission (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  description text
);

create table iam_user_role (
  user_id uuid not null references iam_user(id) on delete cascade,
  role_id uuid not null references iam_role(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, role_id)
);

create table iam_role_permission (
  role_id uuid not null references iam_role(id) on delete cascade,
  permission_id uuid not null references iam_permission(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (role_id, permission_id)
);

create table iam_invitation (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id),
  email_normalized text not null,
  invited_roles jsonb not null default '[]'::jsonb,
  token_hash text not null,
  expires_at timestamptz not null,
  used_at timestamptz,
  created_by_user_id uuid references iam_user(id),
  created_at timestamptz not null default now()
);

create index idx_invite_tenant_email on iam_invitation(tenant_id, email_normalized);

create table iam_refresh_token (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references iam_tenant(id),
  user_id uuid not null references iam_user(id) on delete cascade,
  token_family_id uuid not null,
  token_hash text not null,
  issued_at timestamptz not null default now(),
  expires_at timestamptz not null,
  revoked_at timestamptz,
  replaced_by_token_id uuid,
  ip text,
  user_agent text
);

create index idx_refresh_user_active on iam_refresh_token(user_id, revoked_at, expires_at);

create table iam_audit_event (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid references iam_tenant(id),
  actor_user_id uuid references iam_user(id),
  event_type text not null,
  event_time timestamptz not null default now(),
  ip text,
  user_agent text,
  metadata jsonb not null default '{}'::jsonb
);

create index idx_audit_tenant_time on iam_audit_event(tenant_id, event_time);

insert into iam_permission(code, description) values
('iam.user.read', 'Read users'),
('iam.user.write', 'Create or update users'),
('iam.user.delete', 'Delete users'),
('iam.role.read', 'Read roles'),
('iam.role.write', 'Create or update roles'),
('iam.role.delete', 'Delete roles'),
('iam.invite.write', 'Create invitations'),
('tenant.read', 'Read tenant metadata');
