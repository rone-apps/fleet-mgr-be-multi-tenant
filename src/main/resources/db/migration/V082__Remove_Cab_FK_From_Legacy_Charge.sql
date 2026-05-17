-- Remove cab_id FK constraint from legacy_customer_charge
-- We don't need cab information for charges - only driver_id matters
-- Keep the column for reference but make it nullable and remove FK

ALTER TABLE legacy_customer_charge
  DROP FOREIGN KEY fk_legacy_charge_cab;

ALTER TABLE legacy_customer_charge
  MODIFY COLUMN cab_id BIGINT NULL;

-- Rollback:
-- ALTER TABLE legacy_customer_charge
--   MODIFY COLUMN cab_id BIGINT NOT NULL,
--   ADD CONSTRAINT fk_legacy_charge_cab FOREIGN KEY (cab_id) REFERENCES cab (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
