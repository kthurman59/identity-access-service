ALTER TABLE refresh_token
  DROP CONSTRAINT IF EXISTS fk_refresh_token_replaced_by;

ALTER TABLE refresh_token
  ADD CONSTRAINT fk_refresh_token_replaced_by
  FOREIGN KEY (replaced_by)
  REFERENCES refresh_token(id)
  DEFERRABLE INITIALLY DEFERRED;

