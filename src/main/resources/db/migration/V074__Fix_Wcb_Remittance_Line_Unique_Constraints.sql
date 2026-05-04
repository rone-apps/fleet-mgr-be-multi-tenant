-- V074: Replace over-strict line-item unique constraints with content-hash approach.
--
-- V073 added (remittance_id, claim_number) and (remittance_id, invoice_no) as UNIQUE.
-- These are wrong: the same claim_number appears on every trip for an injured worker,
-- so they prevent legitimate multi-trip entries under the same claim.
--
-- The true duplicate is an identical row re-scanned from the same PDF page.
-- We detect that by hashing all business fields into a single content_hash column.

-- 1. Drop the over-strict V073 unique constraints
ALTER TABLE wcb_remittance_line
    DROP INDEX uk_wcb_line_claim_per_remittance;

ALTER TABLE wcb_remittance_line
    DROP INDEX uk_wcb_line_invoice_per_remittance;

-- 2. Drop the V073 lookup indexes (they will be replaced)
DROP INDEX idx_wcb_line_duplicate_check_claim   ON wcb_remittance_line;
DROP INDEX idx_wcb_line_duplicate_check_invoice ON wcb_remittance_line;

-- 3. Add content_hash column
--    Use CONCAT('legacy-', id) as a unique sentinel for existing rows.
--    This avoids constraint violation while keeping each legacy row unique per remittance.
ALTER TABLE wcb_remittance_line
    ADD COLUMN content_hash VARCHAR(64) NOT NULL DEFAULT '';

UPDATE wcb_remittance_line SET content_hash = CONCAT('legacy-', id) WHERE content_hash = '';

-- 4. Add the new unique constraint: one hash per remittance
ALTER TABLE wcb_remittance_line
    ADD CONSTRAINT uk_wcb_line_hash_per_remittance UNIQUE (remittance_id, content_hash);

-- 5. Index for fast hash lookups
CREATE INDEX idx_wcb_line_content_hash
    ON wcb_remittance_line (remittance_id, content_hash);
