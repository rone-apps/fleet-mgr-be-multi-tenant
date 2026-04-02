-- Simplify to a single natural-key constraint on credit_card_transaction.
-- The synthetic transaction_id doesn't need a unique constraint — it's generated, not from the processor.
-- The true natural key is: merchant + terminal + auth code + exact datetime.

-- Drop all existing unique constraints (use IF EXISTS for safety)
ALTER TABLE credit_card_transaction DROP INDEX IF EXISTS uk_cc_transaction_primary;
ALTER TABLE credit_card_transaction DROP INDEX IF EXISTS uk_cc_transaction_secondary;
ALTER TABLE credit_card_transaction DROP INDEX IF EXISTS uk_cc_transaction_id;
ALTER TABLE credit_card_transaction DROP INDEX IF EXISTS uk_cc_transaction_natural;

-- Make transaction_id nullable (no unique constraint needed)
ALTER TABLE credit_card_transaction
    MODIFY transaction_id varchar(100) NULL COMMENT 'Generated transaction ID (not unique-constrained)';

-- Single natural-key constraint: merchant + terminal + auth code + exact datetime
ALTER TABLE credit_card_transaction
    ADD UNIQUE KEY uk_cc_transaction_natural (merchant_id, terminal_id, authorization_code, transaction_date, transaction_time);
