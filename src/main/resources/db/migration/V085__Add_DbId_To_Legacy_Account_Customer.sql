-- Add db_id column to legacy_account_customer to store original database ID
-- This allows mapping old system IDs to new auto-generated IDs

ALTER TABLE legacy_account_customer
  ADD COLUMN db_id BIGINT UNIQUE COMMENT 'Original database ID from old system';

-- Create index for fast lookups during charge import
CREATE INDEX idx_legacy_customer_db_id ON legacy_account_customer(db_id);

-- Rollback:
-- ALTER TABLE legacy_account_customer DROP COLUMN db_id;
-- DROP INDEX idx_legacy_customer_db_id ON legacy_account_customer;
