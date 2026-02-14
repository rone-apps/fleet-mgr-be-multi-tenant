-- Migration: Add attribute-based expense support
-- Date: 2026-02-11
-- Purpose: Enable expense categories to apply to all shifts with a specific attribute

-- ============================================================================
-- Add attribute_type_id to expense_category
-- ============================================================================

ALTER TABLE expense_category
  ADD COLUMN attribute_type_id BIGINT NULL COMMENT 'Link to attribute type for SHIFTS_WITH_ATTRIBUTE application type';

-- Add foreign key constraint
ALTER TABLE expense_category
  ADD CONSTRAINT fk_expense_category_attribute_type
    FOREIGN KEY (attribute_type_id) REFERENCES cab_attribute_type(id) ON DELETE SET NULL;

-- Create index for performance
CREATE INDEX idx_expense_category_attribute_type ON expense_category(attribute_type_id);

-- ============================================================================
-- Add attribute_type_id to recurring_expense (for tracking)
-- ============================================================================

ALTER TABLE recurring_expense
  ADD COLUMN attribute_type_id BIGINT NULL COMMENT 'Link to attribute type if created from attribute-based category';

-- Add foreign key constraint
ALTER TABLE recurring_expense
  ADD CONSTRAINT fk_recurring_expense_attribute_type
    FOREIGN KEY (attribute_type_id) REFERENCES cab_attribute_type(id) ON DELETE SET NULL;

-- Create index for performance
CREATE INDEX idx_recurring_expense_attribute_type ON recurring_expense(attribute_type_id);
