-- Migration: Add ApplicationType system to revenue_category
-- Date: 2026-02-11
-- Purpose: Align RevenueCategory with ExpenseCategory by adding application type support
--          Enables targeting revenue categories to specific shift profiles, shifts, owners, drivers, etc.

-- ============================================================================
-- Add Application Type and Target Fields to revenue_category
-- ============================================================================

ALTER TABLE revenue_category
  ADD COLUMN application_type VARCHAR(30) NULL COMMENT 'Application type: SHIFT_PROFILE, SPECIFIC_SHIFT, SPECIFIC_OWNER_DRIVER, ALL_ACTIVE_SHIFTS, ALL_NON_OWNER_DRIVERS',
  ADD COLUMN shift_profile_id BIGINT NULL COMMENT 'Link to shift profile for SHIFT_PROFILE type',
  ADD COLUMN specific_shift_id BIGINT NULL COMMENT 'Link to specific shift for SPECIFIC_SHIFT type',
  ADD COLUMN specific_owner_id BIGINT NULL COMMENT 'Link to specific owner for SPECIFIC_OWNER_DRIVER type',
  ADD COLUMN specific_driver_id BIGINT NULL COMMENT 'Link to specific driver for SPECIFIC_OWNER_DRIVER type';

-- ============================================================================
-- Migrate Existing Data
-- ============================================================================

-- Set default application type for all existing revenue categories
UPDATE revenue_category
SET application_type = 'ALL_ACTIVE_SHIFTS'
WHERE application_type IS NULL;

-- ============================================================================
-- Make application_type NOT NULL
-- ============================================================================

ALTER TABLE revenue_category
MODIFY COLUMN application_type VARCHAR(30) NOT NULL;

-- ============================================================================
-- Add Foreign Key Constraints
-- ============================================================================

ALTER TABLE revenue_category
  ADD CONSTRAINT fk_rev_cat_specific_shift
    FOREIGN KEY (specific_shift_id) REFERENCES cab_shift(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_rev_cat_specific_owner
    FOREIGN KEY (specific_owner_id) REFERENCES driver(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_rev_cat_specific_driver
    FOREIGN KEY (specific_driver_id) REFERENCES driver(id) ON DELETE SET NULL;

-- ============================================================================
-- Create Indexes for Performance
-- ============================================================================

CREATE INDEX idx_rev_cat_application_type ON revenue_category(application_type);
CREATE INDEX idx_rev_cat_specific_shift ON revenue_category(specific_shift_id);
CREATE INDEX idx_rev_cat_specific_owner ON revenue_category(specific_owner_id);
CREATE INDEX idx_rev_cat_specific_driver ON revenue_category(specific_driver_id);

-- ============================================================================
-- Note: CHECK constraint not added due to MySQL limitation with FK columns
-- Validation is enforced in application code via @PrePersist/@PreUpdate
-- ============================================================================
