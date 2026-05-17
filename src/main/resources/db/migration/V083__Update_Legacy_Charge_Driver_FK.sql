-- Update legacy_customer_charge.driver_id FK to point to legacy_driver
-- This allows mapping legacy driver IDs to current drivers via driver_number

ALTER TABLE legacy_customer_charge
  DROP FOREIGN KEY fk_legacy_charge_driver;

ALTER TABLE legacy_customer_charge
  ADD CONSTRAINT fk_legacy_charge_driver
  FOREIGN KEY (driver_id) REFERENCES legacy_driver (id)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Rollback:
-- ALTER TABLE legacy_customer_charge
--   DROP FOREIGN KEY fk_legacy_charge_driver;
-- ALTER TABLE legacy_customer_charge
--   ADD CONSTRAINT fk_legacy_charge_driver
--   FOREIGN KEY (driver_id) REFERENCES driver (id)
--   ON DELETE RESTRICT ON UPDATE RESTRICT;
