-- V076: Drop unique constraint on wcb_remittance_line content_hash
-- The content_hash is now just a technical identifier, not a deduplication mechanism.
-- Summary-level deduplication at (payee_number, cheque_number) is the primary guard.

-- Drop the unique constraint
ALTER TABLE wcb_remittance_line
    DROP CONSTRAINT uk_wcb_line_hash_per_remittance;

-- Keep the index for fast lookups if needed
-- The content_hash column itself remains for audit/reference purposes
