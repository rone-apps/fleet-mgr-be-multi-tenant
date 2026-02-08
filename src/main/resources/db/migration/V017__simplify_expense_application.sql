-- Migration: Simplify Expense Application System
-- Date: 2026-02-07
-- Database: MySQL 8.0+ (Simplified for compatibility)
-- Purpose: Replace complex attribute-based matching with 5 simple application types

-- ============================================================================
-- STEP 1: Add new columns for application type system
-- ============================================================================

ALTER TABLE expense_category
  ADD COLUMN application_type VARCHAR(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Application type: SHIFT_PROFILE, SPECIFIC_SHIFT, SPECIFIC_OWNER_DRIVER, ALL_ACTIVE_SHIFTS, ALL_NON_OWNER_DRIVERS',
  ADD COLUMN specific_shift_id BIGINT DEFAULT NULL COMMENT 'ID of specific shift (for SPECIFIC_SHIFT type)',
  ADD COLUMN specific_owner_id BIGINT DEFAULT NULL COMMENT 'ID of owner driver (for SPECIFIC_OWNER_DRIVER type)',
  ADD COLUMN specific_driver_id BIGINT DEFAULT NULL COMMENT 'ID of driver (for SPECIFIC_OWNER_DRIVER type)';

-- ============================================================================
-- STEP 2: Populate application_type for existing categories
-- ============================================================================

-- Set categories with shift_profile_id to SHIFT_PROFILE type
UPDATE expense_category
SET application_type = 'SHIFT_PROFILE'
WHERE shift_profile_id IS NOT NULL AND (application_type IS NULL OR application_type = '');

-- Set all remaining categories to ALL_ACTIVE_SHIFTS
UPDATE expense_category
SET application_type = 'ALL_ACTIVE_SHIFTS'
WHERE application_type IS NULL OR application_type = '';

-- ============================================================================
-- STEP 3: Make application_type NOT NULL
-- ============================================================================

ALTER TABLE expense_category
  MODIFY COLUMN application_type VARCHAR(30) NOT NULL COLLATE utf8mb4_unicode_ci;

-- ============================================================================
-- STEP 4: Add CHECK constraint for valid application types
-- ============================================================================

ALTER TABLE expense_category
  ADD CONSTRAINT chk_application_type_values
    CHECK (application_type IN (
      'SHIFT_PROFILE',
      'SPECIFIC_SHIFT',
      'SPECIFIC_OWNER_DRIVER',
      'ALL_ACTIVE_SHIFTS',
      'ALL_NON_OWNER_DRIVERS'
    ));

-- ============================================================================
-- STEP 5: Add foreign key constraints
-- ============================================================================
-- Note: Cannot use CHECK constraints on foreign key columns in MySQL 8
-- Validation of mutual exclusivity (owner XOR driver) is handled in Java layer

ALTER TABLE expense_category
  ADD CONSTRAINT fk_expense_category_shift
    FOREIGN KEY (specific_shift_id) REFERENCES cab_shift(id) ON DELETE SET NULL;

ALTER TABLE expense_category
  ADD CONSTRAINT fk_expense_category_owner
    FOREIGN KEY (specific_owner_id) REFERENCES driver(id) ON DELETE SET NULL;

ALTER TABLE expense_category
  ADD CONSTRAINT fk_expense_category_driver
    FOREIGN KEY (specific_driver_id) REFERENCES driver(id) ON DELETE SET NULL;

-- ============================================================================
-- STEP 6: Create performance indexes
-- ============================================================================

CREATE INDEX idx_expense_category_app_type ON expense_category(application_type);
CREATE INDEX idx_expense_category_shift ON expense_category(specific_shift_id);
CREATE INDEX idx_expense_category_owner ON expense_category(specific_owner_id);
CREATE INDEX idx_expense_category_driver ON expense_category(specific_driver_id);

-- ============================================================================
-- VERIFICATION QUERIES - Run these manually to verify migration success
-- ============================================================================
-- Note: Deprecated columns (supports_auto_matching, supports_individual_config)
-- are left in place for backward compatibility and can be cleaned up later

-- Check 1: Verify columns exist
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_NAME = 'expense_category' AND TABLE_SCHEMA = DATABASE()
-- AND COLUMN_NAME IN ('application_type', 'specific_shift_id', 'specific_owner_id', 'specific_driver_id');

-- Check 2: Verify data populated
-- SELECT application_type, COUNT(*) FROM expense_category GROUP BY application_type;

-- Check 3: Verify constraints created
-- SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
-- WHERE TABLE_NAME = 'expense_category' AND TABLE_SCHEMA = DATABASE() ORDER BY CONSTRAINT_NAME;

-- Check 4: Verify foreign keys
-- SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
-- WHERE TABLE_NAME = 'expense_category' AND TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Check 5: Verify indexes
-- SHOW INDEX FROM expense_category WHERE Key_name LIKE 'idx_expense_category%';
