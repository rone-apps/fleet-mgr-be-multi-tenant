-- Add unique constraints for WCB remittance line items
-- Each claim_number should be unique within a remittance
ALTER TABLE wcb_remittance_line
ADD CONSTRAINT uk_wcb_line_claim_per_remittance UNIQUE (remittance_id, claim_number);

-- Each invoice_no should be unique within a remittance (as fallback identifier)
ALTER TABLE wcb_remittance_line
ADD CONSTRAINT uk_wcb_line_invoice_per_remittance UNIQUE (remittance_id, invoice_no);

-- Add indexes for duplicate detection lookups
CREATE INDEX idx_wcb_line_duplicate_check_claim
ON wcb_remittance_line (remittance_id, claim_number);

CREATE INDEX idx_wcb_line_duplicate_check_invoice
ON wcb_remittance_line (remittance_id, invoice_no);

-- Log message
-- Migration adds:
-- 1. Unique constraint: (remittance_id, claim_number) - prevents duplicate claims per remittance
-- 2. Unique constraint: (remittance_id, invoice_no) - prevents duplicate invoices per remittance
-- 3. Indexes for fast duplicate detection checks
