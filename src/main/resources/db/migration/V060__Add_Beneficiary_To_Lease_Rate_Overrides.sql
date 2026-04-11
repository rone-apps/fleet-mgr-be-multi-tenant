-- V060: Add beneficiary_driver_number column to lease_rate_overrides
-- Allows setting driver-specific exemptions/rates (e.g., for co-owner arrangements)
-- null = owner-level rate (existing behavior)
-- not null = driver-specific rate (new behavior)

ALTER TABLE lease_rate_overrides
ADD COLUMN beneficiary_driver_number VARCHAR(50) NULL AFTER owner_driver_number;

-- Index for efficient lookup of beneficiary-specific overrides
CREATE INDEX idx_lro_beneficiary ON lease_rate_overrides(beneficiary_driver_number);
