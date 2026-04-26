-- Add updated_by column to track who last modified the receipt
ALTER TABLE receipts
ADD COLUMN updated_by VARCHAR(255) NULL;
