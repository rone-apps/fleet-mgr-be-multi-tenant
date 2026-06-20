-- V110__Add_Mileage_Fields_To_Lease_Rate_Overrides.sql
-- Add support for structured (base + mileage) lease rate overrides

-- Add new columns for base rate and mileage rate overrides
ALTER TABLE lease_rate_overrides
  ADD COLUMN base_rate_override DECIMAL(10,2) NULL COMMENT 'Optional override for base lease rate component',
  ADD COLUMN mileage_rate_override DECIMAL(10,4) NULL COMMENT 'Optional override for per-mile rate component';

-- Backward compatibility notes:
-- Existing records with only 'lease_rate' field are still valid (flat rate mode)
-- New overrides can specify either:
--   1. lease_rate only → flat total (current behavior, no mileage calculation)
--   2. base_rate_override + mileage_rate_override → structured rate (Total = base + miles × mileage)
--   3. Cannot mix: setting both lease_rate AND base/mileage overrides is invalid (enforced in application layer)

-- Example use cases after migration:
-- Flat rate:      lease_rate = 75.00, base_rate_override = NULL, mileage_rate_override = NULL
-- Structured:     lease_rate = NULL, base_rate_override = 50.00, mileage_rate_override = 0.1500
-- Discount:       lease_rate = NULL, base_rate_override = 40.00, mileage_rate_override = 0.1000
-- No mileage:     lease_rate = NULL, base_rate_override = 60.00, mileage_rate_override = 0.0000

-- Rollback:
-- ALTER TABLE lease_rate_overrides DROP COLUMN base_rate_override;
-- ALTER TABLE lease_rate_overrides DROP COLUMN mileage_rate_override;
