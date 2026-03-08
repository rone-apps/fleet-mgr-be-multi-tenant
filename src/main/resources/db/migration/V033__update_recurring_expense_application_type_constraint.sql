-- Migration: Fix recurring_expense application_type column and constraints
-- Date: 2026-03-08
-- Purpose: Ensure application_type column accepts all current ApplicationType enum values

-- Step 1: Ensure the application_type column (used by Hibernate) is VARCHAR(30), not a MySQL ENUM
ALTER TABLE recurring_expense
  MODIFY COLUMN application_type VARCHAR(30) NULL;

-- Step 2: Update CHECK constraint on application_type_enum (Flyway-created column)
ALTER TABLE recurring_expense
  DROP CONSTRAINT IF EXISTS chk_recurring_application_type_values;

ALTER TABLE recurring_expense
  ADD CONSTRAINT chk_recurring_application_type_values
    CHECK (application_type_enum IS NULL OR application_type_enum IN (
      'SHIFT_PROFILE',
      'SPECIFIC_SHIFT',
      'SPECIFIC_PERSON',
      'SPECIFIC_OWNER_DRIVER',
      'ALL_ACTIVE_SHIFTS',
      'ALL_OWNERS',
      'ALL_DRIVERS',
      'ALL_NON_OWNER_DRIVERS',
      'SHIFTS_WITH_ATTRIBUTE'
    ));
