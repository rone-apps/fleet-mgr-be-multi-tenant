-- Migration: Add Application Type to OneTimeExpense
-- Date: 2026-02-08
-- Database: MySQL 8.0+
-- Purpose: Support simplified application type system for one-time expenses
--          (same as fixed/recurring expenses)

-- ============================================================================
-- STEP 1: Add new columns for application type system
-- ============================================================================

ALTER TABLE one_time_expense
  ADD COLUMN application_type VARCHAR(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Application type: SHIFT_PROFILE, SPECIFIC_SHIFT, SPECIFIC_OWNER_DRIVER, ALL_ACTIVE_SHIFTS, ALL_NON_OWNER_DRIVERS',
  ADD COLUMN shift_profile_id BIGINT DEFAULT NULL COMMENT 'ID of shift profile (for SHIFT_PROFILE type)',
  ADD COLUMN specific_shift_id BIGINT DEFAULT NULL COMMENT 'ID of specific shift (for SPECIFIC_SHIFT type)',
  ADD COLUMN specific_owner_id BIGINT DEFAULT NULL COMMENT 'ID of owner driver (for SPECIFIC_OWNER_DRIVER type)',
  ADD COLUMN specific_driver_id BIGINT DEFAULT NULL COMMENT 'ID of driver (for SPECIFIC_OWNER_DRIVER type)';

-- ============================================================================
-- STEP 2: Add foreign key constraints
-- ============================================================================

ALTER TABLE one_time_expense
  ADD CONSTRAINT fk_one_time_expense_shift_profile
    FOREIGN KEY (shift_profile_id) REFERENCES shift_profile(id) ON DELETE SET NULL;

ALTER TABLE one_time_expense
  ADD CONSTRAINT fk_one_time_expense_specific_shift
    FOREIGN KEY (specific_shift_id) REFERENCES cab_shift(id) ON DELETE SET NULL;

ALTER TABLE one_time_expense
  ADD CONSTRAINT fk_one_time_expense_specific_owner
    FOREIGN KEY (specific_owner_id) REFERENCES driver(id) ON DELETE SET NULL;

ALTER TABLE one_time_expense
  ADD CONSTRAINT fk_one_time_expense_specific_driver
    FOREIGN KEY (specific_driver_id) REFERENCES driver(id) ON DELETE SET NULL;

-- ============================================================================
-- STEP 3: Create performance indexes
-- ============================================================================

CREATE INDEX idx_one_time_expense_app_type ON one_time_expense(application_type);
CREATE INDEX idx_one_time_expense_shift_profile ON one_time_expense(shift_profile_id);
CREATE INDEX idx_one_time_expense_specific_shift ON one_time_expense(specific_shift_id);
CREATE INDEX idx_one_time_expense_specific_owner ON one_time_expense(specific_owner_id);
CREATE INDEX idx_one_time_expense_specific_driver ON one_time_expense(specific_driver_id);

-- ============================================================================
-- STEP 4: Add CHECK constraint for valid application types
-- ============================================================================

ALTER TABLE one_time_expense
  ADD CONSTRAINT chk_one_time_application_type_values
    CHECK (application_type IS NULL OR application_type IN (
      'SHIFT_PROFILE',
      'SPECIFIC_SHIFT',
      'SPECIFIC_OWNER_DRIVER',
      'ALL_ACTIVE_SHIFTS',
      'ALL_NON_OWNER_DRIVERS'
    ));

-- ============================================================================
-- NOTE: Legacy columns (entity_type, entity_id) are kept for backward compatibility
-- Both old system and new application type system are supported
-- ============================================================================
