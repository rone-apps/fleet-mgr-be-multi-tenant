-- Make expense_category_id nullable in recurring_expense table
-- Allows recurring expenses to be created from attribute costs without requiring an expense category
-- Attributes are independent and should not require an expense category to be assigned

ALTER TABLE recurring_expense
MODIFY COLUMN expense_category_id BIGINT NULL;
