update iam_user
set password_hash = '{noop}Admin123!'
where username = 'admin'
  and (
    password_hash is null
    or password_hash = ''
    or password_hash like '%REPLACE_WITH_YOUR_BCRYPT_HASH%'
  );

