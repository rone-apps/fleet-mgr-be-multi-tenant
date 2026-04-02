-- Fix false-positive duplicate detection on credit_card_transaction.
--
-- Problem: Three overlapping unique constraints were rejecting legitimate transactions
-- as duplicates (e.g., same cab, two different fares at same amount in same minute).
--
-- Solution: Replace with a single natural-key constraint (terminal + auth code + datetime).
-- A real authorization code is unique per terminal per second — that's how card processing works.
-- Also make transaction_id nullable since migrated/legacy data may not have one.

-- 1. Drop the three problematic unique constraints
ALTER TABLE credit_card_transaction DROP INDEX uk_cc_transaction_primary;
ALTER TABLE credit_card_transaction DROP INDEX uk_cc_transaction_secondary;
ALTER TABLE credit_card_transaction DROP INDEX uk_cc_transaction_id;

-- 2. Make transaction_id nullable (legacy/migrated data won't have a processor-assigned ID)
ALTER TABLE credit_card_transaction
    MODIFY transaction_id varchar(100) NULL COMMENT 'Unique transaction ID from payment processor (NULL for migrated data)';

-- 3. Single robust natural-key constraint: same terminal + auth code + exact datetime = duplicate
ALTER TABLE credit_card_transaction
    ADD UNIQUE KEY uk_cc_transaction_natural (terminal_id, authorization_code, transaction_date, transaction_time);

-- 4. Keep transaction_id unique where it exists (NULL values are ignored by unique indexes in MySQL)
ALTER TABLE credit_card_transaction
    ADD UNIQUE KEY uk_cc_transaction_id (transaction_id);
