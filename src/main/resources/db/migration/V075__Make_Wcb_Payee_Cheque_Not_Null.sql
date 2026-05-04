-- V075: Make payee_number and cheque_number NOT NULL to enforce unique constraint
-- These fields are essential for duplicate detection and should never be null

-- Set default values for any existing null rows (emergency fallback)
UPDATE wcb_remittance SET payee_number = CONCAT('UNKNOWN-', id) WHERE payee_number IS NULL;
UPDATE wcb_remittance SET cheque_number = CONCAT('UNKNOWN-', id) WHERE cheque_number IS NULL;

-- Now add NOT NULL constraint
ALTER TABLE wcb_remittance
    MODIFY COLUMN payee_number VARCHAR(100) NOT NULL;

ALTER TABLE wcb_remittance
    MODIFY COLUMN cheque_number VARCHAR(100) NOT NULL;
