-- Add tenant key column used by Tenant.key

alter table iam_tenant
    add column if not exists key varchar(64);

update iam_tenant
set key = id::text
where key is null;

alter table iam_tenant
    alter column key set not null;

alter table iam_tenant
    add constraint uq_iam_tenant_key unique (key);

