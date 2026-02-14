-- Migration: Add SHIFTS_WITH_ATTRIBUTE to application_type CHECK constraint
-- Date: 2026-02-11
-- Purpose: Support applying expenses to all shifts with a specific attribute

-- ============================================================================
-- Update CHECK constraint for expense_category to include SHIFTS_WITH_ATTRIBUTE
-- ============================================================================

ALTER TABLE expense_category
  DROP CONSTRAINT chk_application_type_values;

ALTER TABLE expense_category
  ADD CONSTRAINT chk_application_type_values
    CHECK (application_type IN (
      'SHIFT_PROFILE',
      'SPECIFIC_SHIFT',
      'SPECIFIC_OWNER_DRIVER',
      'ALL_ACTIVE_SHIFTS',
      'ALL_NON_OWNER_DRIVERS',
      'SHIFTS_WITH_ATTRIBUTE'
    ));

-- ============================================================================
-- Note: revenue_category does NOT have a CHECK constraint
-- Validation is enforced in application code via @PrePersist/@PreUpdate
-- No database constraint update needed
-- ============================================================================

-- ============================================================================
-- Update CHECK constraint for one_time_expense to include SHIFTS_WITH_ATTRIBUTE
-- ============================================================================

ALTER TABLE one_time_expense
  DROP CONSTRAINT chk_one_time_application_type_values;

ALTER TABLE one_time_expense
  ADD CONSTRAINT chk_one_time_application_type_values
    CHECK (application_type IS NULL OR application_type IN (
      'SHIFT_PROFILE',
      'SPECIFIC_SHIFT',
      'SPECIFIC_OWNER_DRIVER',
      'ALL_ACTIVE_SHIFTS',
      'ALL_NON_OWNER_DRIVERS',
      'SHIFTS_WITH_ATTRIBUTE'
    ));
