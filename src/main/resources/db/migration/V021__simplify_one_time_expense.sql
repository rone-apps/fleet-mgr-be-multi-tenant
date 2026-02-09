-- Migration: Simplify OneTimeExpense - Make it Standalone
-- Date: 2026-02-08
-- Database: MySQL 8.0+
-- Purpose: OneTimeExpenses are now standalone, not tied to categories
--          Add name field and make expense_category_id optional

-- ============================================================================
-- STEP 1: Add name column
-- ============================================================================

ALTER TABLE one_time_expense
  ADD COLUMN name VARCHAR(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Unnamed Expense' COMMENT 'Name/title of the one-time expense';

-- ============================================================================
-- STEP 2: Make expense_category_id optional
-- ============================================================================

ALTER TABLE one_time_expense
  MODIFY COLUMN expense_category_id BIGINT DEFAULT NULL COMMENT 'Optional: Associated expense category (for legacy compatibility)';

-- ============================================================================
-- STEP 3: Create index on name for search
-- ============================================================================

CREATE INDEX idx_one_time_expense_name ON one_time_expense(name);

-- ============================================================================
-- NOTE: One-time expenses are now standalone
-- expense_category_id is kept for backward compatibility but is not required
-- ============================================================================
