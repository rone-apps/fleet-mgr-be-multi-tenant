-- Add extended attributes to driver table
-- Includes: Tax & Financial Information, Emergency Contact, and Document Dates

-- Tax & Financial Information columns
ALTER TABLE `driver` ADD COLUMN `sin` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Social Insurance Number';
ALTER TABLE `driver` ADD COLUMN `gst_number` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'GST/HST Number';
ALTER TABLE `driver` ADD COLUMN `deposit_amount` DOUBLE DEFAULT NULL COMMENT 'Security deposit amount';

-- Emergency Contact Information columns
ALTER TABLE `driver` ADD COLUMN `emergency_contact_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Emergency contact person name';
ALTER TABLE `driver` ADD COLUMN `emergency_contact_phone` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Emergency contact phone number';
ALTER TABLE `driver` ADD COLUMN `emergency_contact_relationship` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Relationship to driver (Spouse, Parent, etc.)';

-- Document & Record Dates columns
ALTER TABLE `driver` ADD COLUMN `security_deposit_date` DATE DEFAULT NULL COMMENT 'Date security deposit was received';
ALTER TABLE `driver` ADD COLUMN `refund_date` DATE DEFAULT NULL COMMENT 'Date security deposit was refunded';
ALTER TABLE `driver` ADD COLUMN `pic_date` DATE DEFAULT NULL COMMENT 'PIC (Police Information Check) record date';
ALTER TABLE `driver` ADD COLUMN `ibc_records_date` DATE DEFAULT NULL COMMENT 'ICBC (Insurance Corporation of British Columbia) records date';

-- Add indexes for better query performance on commonly searched date fields
ALTER TABLE `driver` ADD INDEX `idx_driver_security_deposit_date` (`security_deposit_date`);
ALTER TABLE `driver` ADD INDEX `idx_driver_pic_date` (`pic_date`);
ALTER TABLE `driver` ADD INDEX `idx_driver_ibc_records_date` (`ibc_records_date`);
