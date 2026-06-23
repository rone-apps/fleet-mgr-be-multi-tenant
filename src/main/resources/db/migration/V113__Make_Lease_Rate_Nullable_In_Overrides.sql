-- V113__Make_Lease_Rate_Nullable_In_Overrides.sql
-- Fix lease_rate column to allow NULL in structured mode (base + mileage)
-- This aligns with V110 design where EITHER lease_rate OR base+mileage is required

-- Make lease_rate nullable to support structured mode
ALTER TABLE lease_rate_overrides
  MODIFY COLUMN lease_rate DECIMAL(10,2) NULL COMMENT 'Flat rate override (NULL when using structured mode)';

-- Explanation:
-- Flat rate mode:      lease_rate = 75.00, base_rate_override = NULL, mileage_rate_override = NULL
-- Structured mode:     lease_rate = NULL, base_rate_override = 50.00, mileage_rate_override = 0.1500
-- Application layer validates that exactly one mode is used (enforced in LeaseRateOverride.validate())

-- Rollback:
-- ALTER TABLE lease_rate_overrides MODIFY COLUMN lease_rate DECIMAL(10,2) NOT NULL;
