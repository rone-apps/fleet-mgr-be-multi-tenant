-- V059: Add is_active column to spare_machine table
-- Allows soft delete/deactivation while preserving history

ALTER TABLE spare_machine
ADD COLUMN is_active BOOLEAN DEFAULT TRUE AFTER merchant_number;
