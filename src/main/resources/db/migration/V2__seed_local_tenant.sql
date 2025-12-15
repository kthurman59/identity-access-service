insert into iam_tenant(slug, name) values ('local', 'Local Tenant');

insert into iam_role(tenant_id, name, description, is_system)
select id, 'TENANT_ADMIN', 'Tenant administrator', true
from iam_tenant where slug = 'local';

insert into iam_role(tenant_id, name, description, is_system)
select id, 'USER', 'Standard user', true
from iam_tenant where slug = 'local';

insert into iam_role_permission(role_id, permission_id)
select r.id, p.id
from iam_role r
join iam_tenant t on t.id = r.tenant_id
join iam_permission p on p.code in (
  'iam.user.read','iam.user.write','iam.user.delete',
  'iam.role.read','iam.role.write','iam.role.delete',
  'iam.invite.write','tenant.read'
)
where t.slug = 'local' and r.name = 'TENANT_ADMIN';

insert into iam_role_permission(role_id, permission_id)
select r.id, p.id
from iam_role r
join iam_tenant t on t.id = r.tenant_id
join iam_permission p on p.code in ('tenant.read')
where t.slug = 'local' and r.name = 'USER';
