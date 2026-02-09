-- Migration: Add Application Type to RecurringExpense
-- Date: 2026-02-08
-- Database: MySQL 8.0+
-- Purpose: Support simplified application type system for recurring expenses
--          (same as one-time expenses)

-- ============================================================================
-- STEP 1: Add new columns for application type system
-- ============================================================================

ALTER TABLE recurring_expense
  ADD COLUMN application_type_enum VARCHAR(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Application type: SHIFT_PROFILE, SPECIFIC_SHIFT, SPECIFIC_OWNER_DRIVER, ALL_ACTIVE_SHIFTS, ALL_NON_OWNER_DRIVERS',
  ADD COLUMN shift_profile_id BIGINT DEFAULT NULL COMMENT 'ID of shift profile (for SHIFT_PROFILE type)',
  ADD COLUMN specific_shift_id BIGINT DEFAULT NULL COMMENT 'ID of specific shift (for SPECIFIC_SHIFT type)',
  ADD COLUMN specific_owner_id BIGINT DEFAULT NULL COMMENT 'ID of owner driver (for SPECIFIC_OWNER_DRIVER type)',
  ADD COLUMN specific_driver_id BIGINT DEFAULT NULL COMMENT 'ID of driver (for SPECIFIC_OWNER_DRIVER type)';

-- ============================================================================
-- STEP 2: Add foreign key constraints
-- ============================================================================

ALTER TABLE recurring_expense
  ADD CONSTRAINT fk_recurring_expense_shift_profile
    FOREIGN KEY (shift_profile_id) REFERENCES shift_profile(id) ON DELETE SET NULL;

ALTER TABLE recurring_expense
  ADD CONSTRAINT fk_recurring_expense_specific_shift
    FOREIGN KEY (specific_shift_id) REFERENCES cab_shift(id) ON DELETE SET NULL;

ALTER TABLE recurring_expense
  ADD CONSTRAINT fk_recurring_expense_specific_owner
    FOREIGN KEY (specific_owner_id) REFERENCES driver(id) ON DELETE SET NULL;

ALTER TABLE recurring_expense
  ADD CONSTRAINT fk_recurring_expense_specific_driver
    FOREIGN KEY (specific_driver_id) REFERENCES driver(id) ON DELETE SET NULL;

-- ============================================================================
-- STEP 3: Create performance indexes
-- ============================================================================

CREATE INDEX idx_recurring_expense_app_type ON recurring_expense(application_type_enum);
CREATE INDEX idx_recurring_expense_shift_profile ON recurring_expense(shift_profile_id);
CREATE INDEX idx_recurring_expense_specific_shift ON recurring_expense(specific_shift_id);
CREATE INDEX idx_recurring_expense_specific_owner ON recurring_expense(specific_owner_id);
CREATE INDEX idx_recurring_expense_specific_driver ON recurring_expense(specific_driver_id);

-- ============================================================================
-- STEP 4: Add CHECK constraint for valid application types
-- ============================================================================

ALTER TABLE recurring_expense
  ADD CONSTRAINT chk_recurring_application_type_values
    CHECK (application_type_enum IS NULL OR application_type_enum IN (
      'SHIFT_PROFILE',
      'SPECIFIC_SHIFT',
      'SPECIFIC_OWNER_DRIVER',
      'ALL_ACTIVE_SHIFTS',
      'ALL_NON_OWNER_DRIVERS'
    ));

-- ============================================================================
-- STEP 5: Make legacy entity fields nullable
-- ============================================================================
-- These columns are only used by the legacy entity type system
-- New application type system does not require them

ALTER TABLE recurring_expense
  MODIFY COLUMN entity_id BIGINT DEFAULT NULL COMMENT 'Legacy: ID of associated entity (for backward compatibility)',
  MODIFY COLUMN entity_type VARCHAR(20) DEFAULT NULL COMMENT 'Legacy: Type of associated entity (for backward compatibility)';

-- ============================================================================
-- NOTE: Going forward, new recurring expenses should use:
-- - application_type_enum, shift_profile_id, specific_shift_id, specific_owner_id, specific_driver_id
--
-- Legacy system (entity_type, entity_id) is maintained for backward compatibility
-- ============================================================================
