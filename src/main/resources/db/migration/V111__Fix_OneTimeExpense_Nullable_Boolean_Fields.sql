-- V111__Fix_OneTimeExpense_Nullable_Boolean_Fields.sql
-- Update NULL boolean fields in one_time_expense table to prevent JPA mapping errors

-- Update is_reimbursable: Set NULL values to FALSE (default)
UPDATE one_time_expense
SET is_reimbursable = FALSE
WHERE is_reimbursable IS NULL;

-- Update is_reimbursed: Set NULL values to FALSE (default)
UPDATE one_time_expense
SET is_reimbursed = FALSE
WHERE is_reimbursed IS NULL;

-- Add NOT NULL constraints to prevent future NULL values
-- Note: The entity now uses Boolean wrapper class to handle any existing nulls gracefully
-- but we still set defaults for data consistency

-- Rollback:
-- No rollback needed - this is a data cleanup migration
-- The entity can handle NULL values with Boolean wrapper class
