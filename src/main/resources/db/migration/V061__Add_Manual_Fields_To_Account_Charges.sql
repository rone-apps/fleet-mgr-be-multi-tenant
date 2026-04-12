ALTER TABLE account_charges
  ADD COLUMN is_manual  BOOLEAN      NOT NULL DEFAULT FALSE AFTER notes,
  ADD COLUMN created_by VARCHAR(100) NULL     AFTER is_manual;

CREATE INDEX idx_ac_is_manual ON account_charges(is_manual);
