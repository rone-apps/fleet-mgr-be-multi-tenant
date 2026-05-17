-- Update legacy_customer_charge FK to point to legacy_account_customer instead of account_customer
-- This is necessary because legacy and modern customer IDs are different

-- Drop existing FK constraint to account_customer
ALTER TABLE legacy_customer_charge DROP FOREIGN KEY fk_legacy_charge_customer;

-- Add new FK constraint to legacy_account_customer
ALTER TABLE legacy_customer_charge
ADD CONSTRAINT fk_legacy_charge_legacy_customer
FOREIGN KEY (customer_id) REFERENCES legacy_account_customer (id)
ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Rollback:
-- ALTER TABLE legacy_customer_charge DROP FOREIGN KEY fk_legacy_charge_legacy_customer;
-- ALTER TABLE legacy_customer_charge
-- ADD CONSTRAINT fk_legacy_charge_customer
-- FOREIGN KEY (customer_id) REFERENCES account_customer (id)
-- ON DELETE RESTRICT ON UPDATE RESTRICT;
