-- Make total_amount and tax_amount nullable in receipts table
-- These fields are populated during receipt confirmation, not during initial image upload
ALTER TABLE receipts MODIFY COLUMN total_amount DECIMAL(10, 2) NULL;
ALTER TABLE receipts MODIFY COLUMN tax_amount DECIMAL(10, 2) NULL;
