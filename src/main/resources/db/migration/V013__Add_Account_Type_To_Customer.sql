-- Add account_type column to account_customer table
-- Account types: ONE_TIME_USER, PARTY_ACCOUNT, CORPORATE, PERSONAL

ALTER TABLE `account_customer`
ADD COLUMN `account_type` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
DEFAULT 'PERSONAL' COMMENT 'Account type: ONE_TIME_USER, PARTY_ACCOUNT, CORPORATE, PERSONAL';

-- Add index for filtering by account type
ALTER TABLE `account_customer`
ADD INDEX `idx_customer_account_type` (`account_type`);
