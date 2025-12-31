CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE iam_user
SET password_hash = '{bcrypt}' || crypt('Admin123!', gen_salt('bf')),
    updated_at = now()
WHERE username = 'admin';
