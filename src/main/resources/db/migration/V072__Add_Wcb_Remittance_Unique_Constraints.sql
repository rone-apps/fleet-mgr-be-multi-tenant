-- Add unique constraint to prevent duplicate WCB remittances
-- Each (payee_number, cheque_number) combination should be unique
ALTER TABLE wcb_remittance
ADD CONSTRAINT uk_wcb_payee_cheque UNIQUE (payee_number, cheque_number);

-- Add index for duplicate detection lookups
CREATE INDEX idx_wcb_remittance_duplicate_check
ON wcb_remittance (payee_number, cheque_number, receipt_id);

-- Add constraint to ensure receipt_id references are valid
ALTER TABLE wcb_remittance
ADD CONSTRAINT fk_wcb_receipt_valid
FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE;

-- Log message
-- Migration adds:
-- 1. Unique constraint: (payee_number, cheque_number) - prevents exact duplicates
-- 2. Index for fast duplicate detection checks
-- 3. Explicit FK constraint with cascade delete
