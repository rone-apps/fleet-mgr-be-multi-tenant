-- Add cardholder number and capture method to credit card transactions.

-- Masked cardholder number (e.g., 000000452005***2498) — PCI-safe, useful for matching disputes
ALTER TABLE credit_card_transaction
    ADD COLUMN cardholder_number varchar(30) NULL COMMENT 'Masked card number from processor (e.g., 000000452005***2498)' AFTER card_brand;

-- Capture method (O=Online, M=Manual, etc.)
ALTER TABLE credit_card_transaction
    ADD COLUMN capture_method varchar(5) NULL COMMENT 'Card capture method (O=Online, M=Manual)' AFTER reference_number;

-- Index on cardholder_number for dispute lookup
ALTER TABLE credit_card_transaction
    ADD INDEX idx_cc_cardholder_number (cardholder_number);
