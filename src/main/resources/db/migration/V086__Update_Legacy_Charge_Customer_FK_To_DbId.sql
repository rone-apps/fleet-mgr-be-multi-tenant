-- Update legacy_customer_charge.customer_id FK to reference legacy_account_customer.db_id
-- This allows direct column mapping in DB import tool without needing JOINs
-- old_system.customercharges.customerid → legacy_customer_charge.customer_id → legacy_account_customer.db_id

-- Drop old FK constraint
ALTER TABLE legacy_customer_charge
  DROP FOREIGN KEY fk_legacy_charge_legacy_customer;

-- Change customer_id column type if needed (should match db_id type)
ALTER TABLE legacy_customer_charge
  MODIFY COLUMN customer_id BIGINT DEFAULT NULL;

-- Add new FK constraint pointing to db_id
ALTER TABLE legacy_customer_charge
  ADD CONSTRAINT fk_legacy_charge_legacy_customer
  FOREIGN KEY (customer_id) REFERENCES legacy_account_customer (db_id)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Rollback:
-- ALTER TABLE legacy_customer_charge DROP FOREIGN KEY fk_legacy_charge_legacy_customer;
-- ALTER TABLE legacy_customer_charge
--   ADD CONSTRAINT fk_legacy_charge_legacy_customer
--   FOREIGN KEY (customer_id) REFERENCES legacy_account_customer (id)
--   ON DELETE RESTRICT ON UPDATE RESTRICT;
