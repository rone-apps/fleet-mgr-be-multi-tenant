-- Add shift_type column back to receipts table
ALTER TABLE receipts
ADD COLUMN shift_type VARCHAR(50) NULL;
