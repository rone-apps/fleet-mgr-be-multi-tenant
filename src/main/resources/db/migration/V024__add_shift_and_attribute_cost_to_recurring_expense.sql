-- Migration: Add shift and attribute cost tracking to recurring_expense
-- Date: 2026-02-11
-- Purpose: Enable shift-level recurring expenses and track attribute cost associations

-- ============================================================================
-- Add Shift and Attribute Cost columns
-- ============================================================================

ALTER TABLE recurring_expense
  ADD COLUMN shift_id BIGINT NULL COMMENT 'Link to specific shift for shift-level expenses',
  ADD COLUMN attribute_cost_id BIGINT NULL COMMENT 'Link to attribute cost if created from attribute cost';

-- ============================================================================
-- Add Foreign Key Constraint for Shift
-- ============================================================================

ALTER TABLE recurring_expense
  ADD CONSTRAINT fk_recurring_expense_shift
    FOREIGN KEY (shift_id) REFERENCES cab_shift(id) ON DELETE SET NULL;

-- ============================================================================
-- Create Indexes for Performance
-- ============================================================================

CREATE INDEX idx_recurring_expense_shift ON recurring_expense(shift_id);
CREATE INDEX idx_recurring_expense_attribute_cost ON recurring_expense(attribute_cost_id);

-- ============================================================================
-- Add Unique Constraint to prevent duplicate expenses for shift + attribute cost
-- Note: WHERE clause removed for compatibility with older MySQL versions
-- Application code will enforce that both fields are set when needed
-- ============================================================================

ALTER TABLE recurring_expense
  ADD CONSTRAINT uq_recurring_expense_shift_attribute
    UNIQUE (shift_id, attribute_cost_id);


ALTER TABLE expense_category                                                                                       
  ADD COLUMN attribute_type_id BIGINT NULL COMMENT 'Link to attribute type for SHIFTS_WITH_ATTRIBUTE application type';   
