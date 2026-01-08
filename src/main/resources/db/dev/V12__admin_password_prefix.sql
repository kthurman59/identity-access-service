-- Dev only: ensure the seeded admin password is stored with an encoder prefix.
-- Do not touch updated_at because the schema does not have it.

update iam_user
set password_hash = '{bcrypt}' || password_hash
where username = 'admin'
  and password_hash not like '{%';

