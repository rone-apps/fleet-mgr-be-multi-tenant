-- ============================================================================
-- FareFlow New Tenant Bootstrap Script
-- Generated: 2026-04-01
-- Usage: Replace 'fareflow_new' with the actual tenant schema name
--        e.g. fareflow_bonny, fareflow_yellow, etc.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `fareflow_new`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `fareflow_new`;

-- ============================================================================
-- SECTION 1: Core Tables (no foreign key dependencies)
-- ============================================================================

CREATE TABLE `account_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` varchar(50) NOT NULL,
  `company_name` varchar(200) NOT NULL,
  `contact_person` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `street_address` varchar(200) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `province` varchar(50) DEFAULT NULL,
  `country` varchar(50) DEFAULT NULL,
  `postal_code` varchar(20) DEFAULT NULL,
  `billing_period` varchar(20) DEFAULT NULL,
  `credit_limit` decimal(10,2) DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `account_type` enum('CORPORATE','ONE_TIME_USER','PARTY_ACCOUNT','PERSONAL') DEFAULT 'PERSONAL',
  `notes` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_account_customer_company` (`company_name`),
  KEY `idx_charge_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `driver` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `driver_number` varchar(20) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `license_number` varchar(50) DEFAULT NULL,
  `license_expiry` date DEFAULT NULL,
  `joined_date` date DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE','SUSPENDED','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
  `is_owner` bit NOT NULL DEFAULT b'0',
  `is_admin` bit DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `sin` varchar(50) DEFAULT NULL,
  `gst_number` varchar(50) DEFAULT NULL,
  `deposit_amount` double DEFAULT NULL,
  `emergency_contact_name` varchar(100) DEFAULT NULL,
  `emergency_contact_phone` varchar(20) DEFAULT NULL,
  `emergency_contact_relationship` varchar(50) DEFAULT NULL,
  `security_deposit_date` date DEFAULT NULL,
  `refund_date` date DEFAULT NULL,
  `pic_date` date DEFAULT NULL,
  `ibc_records_date` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_driver_number` (`driver_number`),
  UNIQUE KEY `UK_driver_username` (`username`),
  KEY `idx_driver_status` (`status`),
  KEY `idx_driver_name` (`last_name`, `first_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lease_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_name` varchar(100) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_plan_dates` (`effective_from`, `effective_to`),
  KEY `idx_plan_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cab_attribute_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attribute_code` varchar(50) NOT NULL,
  `attribute_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `category` enum('CERTIFICATION','EQUIPMENT','LICENSE','PERMIT','TYPE') NOT NULL,
  `data_type` enum('BOOLEAN','DATE','NUMBER','STRING') NOT NULL,
  `requires_value` bit DEFAULT b'0',
  `validation_pattern` varchar(255) DEFAULT NULL,
  `help_text` varchar(500) DEFAULT NULL,
  `is_active` bit DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_attribute_code` (`attribute_code`),
  KEY `idx_attr_type_active` (`is_active`),
  KEY `idx_attr_type_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payment_method` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `method_name` varchar(100) NOT NULL,
  `method_code` varchar(20) NOT NULL,
  `requires_reference` bit NOT NULL DEFAULT b'1',
  `requires_bank_details` bit NOT NULL DEFAULT b'0',
  `is_automatic` bit NOT NULL DEFAULT b'0',
  `is_active` bit NOT NULL DEFAULT b'1',
  `display_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_method_code` (`method_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tax_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_tax_type_code` (`code`),
  KEY `idx_tax_type_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `commission_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_commission_type_code` (`code`),
  KEY `idx_commission_type_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tenant_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(63) NOT NULL,
  `company_name` varchar(100) DEFAULT NULL,
  `taxicaller_api_key` varchar(100) DEFAULT NULL,
  `taxicaller_company_id` int DEFAULT NULL,
  `taxicaller_base_url` varchar(255) DEFAULT 'https://api.taxicaller.net',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `moneris_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_number` varchar(20) NOT NULL,
  `shift` varchar(10) NOT NULL DEFAULT 'BOTH',
  `merchant_number` varchar(100) NOT NULL,
  `moneris_store_id` varchar(50) NOT NULL,
  `moneris_api_token` varchar(100) NOT NULL,
  `moneris_environment` varchar(10) NOT NULL DEFAULT 'PROD',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moneris_cab_shift_merchant` (`cab_number`, `shift`, `merchant_number`),
  INDEX `idx_moneris_cab` (`cab_number`),
  INDEX `idx_moneris_store` (`moneris_store_id`),
  INDEX `idx_moneris_merchant` (`merchant_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `year_end_report_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `section` varchar(20) NOT NULL,
  `item_key` varchar(100) NOT NULL,
  `item_label` varchar(200) NOT NULL,
  `is_visible` tinyint(1) NOT NULL DEFAULT 1,
  `display_order` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_yer_section_key` (`section`, `item_key`),
  INDEX `idx_yer_section` (`section`),
  INDEX `idx_yer_visible` (`is_visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed year-end report config defaults
INSERT INTO `year_end_report_config` (`section`, `item_key`, `item_label`, `is_visible`, `display_order`, `created_at`) VALUES
('REVENUE', 'LEASE_REVENUE',        'Lease Revenue',                1, 10, NOW()),
('REVENUE', 'CREDIT_CARD_REVENUE',  'Credit Card Revenue',          1, 20, NOW()),
('REVENUE', 'CHARGES_REVENUE',      'Account Charges Revenue',      1, 30, NOW()),
('REVENUE', 'OTHER_REVENUE',        'Other Revenue',                1, 40, NOW()),
('EXPENSE', 'LEASE_EXPENSE',        'Lease Expense',                1, 10, NOW()),
('EXPENSE', 'FIXED_EXPENSE',        'Fixed Expenses',               1, 20, NOW()),
('EXPENSE', 'VARIABLE_EXPENSE',     'Variable / One-Time Expenses', 1, 30, NOW()),
('EXPENSE', 'INSURANCE_MILEAGE',    'Insurance & Mileage',          1, 40, NOW()),
('EXPENSE', 'AIRPORT_TRIPS',        'Airport Trip Charges',         1, 50, NOW()),
('TAX',        'TAX_EXPENSE',        'Taxes (HST/GST)',             1, 10, NOW()),
('COMMISSION', 'COMMISSION_EXPENSE', 'Commissions',                 1, 10, NOW()),
('SUMMARY', 'TOTAL_REVENUE',   'Total Revenue',        1, 10, NOW()),
('SUMMARY', 'TOTAL_EXPENSE',   'Total Expenses',       1, 20, NOW()),
('SUMMARY', 'NET_INCOME',      'Net Income',           1, 30, NOW()),
('SUMMARY', 'PREVIOUS_BALANCE','Previous Balance',     1, 40, NOW()),
('SUMMARY', 'PAYMENTS_MADE',   'Payments Made',        1, 50, NOW()),
('SUMMARY', 'OUTSTANDING',     'Outstanding Balance',  1, 60, NOW());

-- ============================================================================
-- SECTION 2: Tables with single-level FK dependencies
-- ============================================================================

CREATE TABLE `cab` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_number` varchar(20) NOT NULL,
  `registration_number` varchar(50) NOT NULL,
  `make` varchar(50) DEFAULT NULL,
  `model` varchar(50) DEFAULT NULL,
  `year` int DEFAULT NULL,
  `color` varchar(30) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `owner_driver_id` bigint DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cab_number` (`cab_number`),
  KEY `idx_cab_owner_driver` (`owner_driver_id`),
  CONSTRAINT `fk_cab_owner_driver_id` FOREIGN KEY (`owner_driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profile_code` varchar(50) NOT NULL,
  `profile_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `cab_type` enum('HANDICAP_VAN','SEDAN') DEFAULT NULL,
  `share_type` enum('NON_VOTING_SHARE','VOTING_SHARE') DEFAULT NULL,
  `has_airport_license` bit DEFAULT NULL,
  `shift_type` enum('DAY','NIGHT') DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `color_code` varchar(10) DEFAULT NULL,
  `display_order` int DEFAULT 0,
  `is_active` bit DEFAULT b'1',
  `is_system_profile` bit DEFAULT b'0',
  `usage_count` int DEFAULT 0,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_profile_code` (`profile_code`),
  KEY `idx_profile_code` (`profile_code`),
  KEY `idx_profile_active` (`is_active`),
  KEY `idx_profile_category` (`category`),
  KEY `idx_profile_cab_type` (`cab_type`),
  KEY `idx_profile_share_type` (`share_type`),
  KEY `idx_profile_shift_type` (`shift_type`),
  KEY `idx_profile_system` (`is_system_profile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_profile_attribute` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profile_id` bigint NOT NULL,
  `attribute_type_id` bigint NOT NULL,
  `is_required` bit DEFAULT b'0',
  `expected_value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_profile_attribute` (`profile_id`, `attribute_type_id`),
  KEY `idx_profile_attr_profile` (`profile_id`),
  KEY `idx_profile_attr_type` (`attribute_type_id`),
  CONSTRAINT `fk_profile_attr_profile` FOREIGN KEY (`profile_id`) REFERENCES `shift_profile` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_profile_attr_type` FOREIGN KEY (`attribute_type_id`) REFERENCES `cab_attribute_type` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `attribute_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attribute_type_id` bigint NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `billing_unit` enum('DAILY','MONTHLY') NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_attribute_cost_date_range` (`attribute_type_id`, `effective_from`),
  KEY `idx_attribute_cost_attribute_type` (`attribute_type_id`),
  KEY `idx_attribute_cost_effective_from` (`effective_from`),
  KEY `idx_attribute_cost_effective_to` (`effective_to`),
  KEY `idx_attribute_cost_date_range` (`effective_from`, `effective_to`),
  CONSTRAINT `fk_attribute_cost_type` FOREIGN KEY (`attribute_type_id`) REFERENCES `cab_attribute_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `first_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `role` enum('ACCOUNTANT','ADMIN','DISPATCHER','DRIVER','MANAGER','SUPER_ADMIN','VIEWER') NOT NULL,
  `is_active` bit DEFAULT b'1',
  `driver_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_user_username` (`username`),
  UNIQUE KEY `UK_user_email` (`email`),
  UNIQUE KEY `UK_user_driver` (`driver_id`),
  KEY `idx_user_username` (`username`),
  KEY `idx_user_email` (`email`),
  KEY `idx_user_role` (`role`),
  CONSTRAINT `fk_user_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lease_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `cab_type` enum('HANDICAP_VAN','SEDAN') NOT NULL,
  `shift_type` enum('DAY','NIGHT') NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,
  `has_airport_license` bit NOT NULL DEFAULT b'0',
  `base_rate` decimal(10,2) NOT NULL,
  `mileage_rate` decimal(10,4) NOT NULL DEFAULT 0.0000,
  `notes` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_rate_plan_combo` (`plan_id`, `cab_type`, `has_airport_license`, `shift_type`, `day_of_week`),
  KEY `idx_rate_lookup` (`cab_type`, `has_airport_license`, `shift_type`, `day_of_week`),
  CONSTRAINT `fk_lease_rate_plan` FOREIGN KEY (`plan_id`) REFERENCES `lease_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tax_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tax_type_id` bigint NOT NULL,
  `rate` decimal(8,4) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tax_rate_type` (`tax_type_id`),
  KEY `idx_tax_rate_active` (`is_active`),
  KEY `idx_tax_rate_effective` (`effective_from`, `effective_to`),
  CONSTRAINT `fk_tax_rate_type` FOREIGN KEY (`tax_type_id`) REFERENCES `tax_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `commission_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `commission_type_id` bigint NOT NULL,
  `rate` decimal(8,4) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comm_rate_type` (`commission_type_id`),
  KEY `idx_comm_rate_active` (`is_active`),
  KEY `idx_comm_rate_effective` (`effective_from`, `effective_to`),
  CONSTRAINT `fk_comm_rate_type` FOREIGN KEY (`commission_type_id`) REFERENCES `commission_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bank_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `driver_id` bigint NOT NULL,
  `account_holder_name` varchar(30) NOT NULL,
  `institution_number` varchar(4) NOT NULL,
  `transit_number` varchar(5) NOT NULL,
  `account_number` varchar(12) NOT NULL,
  `account_type` varchar(10) NOT NULL DEFAULT 'CHEQUING',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_account_driver_active` (`driver_id`, `is_active`),
  KEY `idx_bank_account_driver` (`driver_id`),
  CONSTRAINT `fk_bank_account_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `eft_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `originator_id` varchar(10) NOT NULL,
  `originator_short_name` varchar(15) NOT NULL,
  `originator_long_name` varchar(30) NOT NULL,
  `processing_centre` varchar(5) DEFAULT '',
  `currency_code` varchar(3) NOT NULL DEFAULT 'CAD',
  `transaction_code` varchar(3) NOT NULL DEFAULT '200',
  `return_institution_id` varchar(4) NOT NULL,
  `return_transit_number` varchar(5) NOT NULL,
  `return_account_number` varchar(12) NOT NULL,
  `file_creation_number` int NOT NULL DEFAULT 1,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eft_config_originator` (`originator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- SECTION 3: Tables with multi-level FK dependencies
-- ============================================================================

CREATE TABLE `cab_shift` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_id` bigint NOT NULL,
  `shift_type` enum('DAY','NIGHT') NOT NULL,
  `start_time` varchar(10) NOT NULL,
  `end_time` varchar(10) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `cab_type` enum('HANDICAP_VAN','SEDAN') DEFAULT NULL,
  `share_type` enum('NON_VOTING_SHARE','VOTING_SHARE') DEFAULT NULL,
  `has_airport_license` bit DEFAULT NULL,
  `airport_license_number` varchar(50) DEFAULT NULL,
  `airport_license_expiry` date DEFAULT NULL,
  `current_owner_id` bigint DEFAULT NULL,
  `current_profile_id` bigint DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cab_shift_type` (`cab_id`, `shift_type`),
  KEY `idx_shift_owner` (`current_owner_id`),
  KEY `idx_shift_status` (`status`),
  KEY `idx_shift_profile_id` (`current_profile_id`),
  CONSTRAINT `fk_cab_shift_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_cab_shift_owner` FOREIGN KEY (`current_owner_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_cab_shift_profile` FOREIGN KEY (`current_profile_id`) REFERENCES `shift_profile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cab_attribute_value` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_id` bigint NOT NULL,
  `attribute_type_id` bigint NOT NULL,
  `shift_id` bigint DEFAULT NULL,
  `attribute_value` varchar(255) DEFAULT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attr_val_cab` (`cab_id`),
  KEY `idx_attr_val_type` (`attribute_type_id`),
  KEY `idx_attr_val_dates` (`start_date`, `end_date`),
  KEY `idx_attr_val_cab_type` (`cab_id`, `attribute_type_id`),
  KEY `idx_attr_val_current` (`cab_id`, `end_date`),
  CONSTRAINT `fk_attr_val_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attr_val_type` FOREIGN KEY (`attribute_type_id`) REFERENCES `cab_attribute_type` (`id`),
  CONSTRAINT `fk_attr_val_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cab_owner_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_id` bigint NOT NULL,
  `owner_driver_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cab_owner_cab_id` (`cab_id`),
  KEY `idx_cab_owner_driver_id` (`owner_driver_id`),
  KEY `idx_cab_owner_start_date` (`start_date`),
  CONSTRAINT `fk_cab_owner_hist_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_cab_owner_hist_driver` FOREIGN KEY (`owner_driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_profile_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift_id` bigint NOT NULL,
  `profile_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `assigned_by` varchar(100) DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shift_active_profile` (`shift_id`, `end_date`),
  KEY `idx_assignment_shift` (`shift_id`),
  KEY `idx_assignment_profile` (`profile_id`),
  KEY `idx_assignment_dates` (`start_date`, `end_date`),
  KEY `idx_assignment_active` (`shift_id`, `end_date`),
  CONSTRAINT `fk_assignment_profile` FOREIGN KEY (`profile_id`) REFERENCES `shift_profile` (`id`),
  CONSTRAINT `fk_assignment_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_status_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift_id` bigint NOT NULL,
  `is_active` bit NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `changed_by` varchar(100) DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shift_status_shift_id` (`shift_id`),
  KEY `idx_shift_status_dates` (`effective_from`, `effective_to`),
  KEY `idx_shift_status_active` (`shift_id`, `is_active`, `effective_from`, `effective_to`),
  CONSTRAINT `fk_shift_status_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_ownership` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `acquisition_type` enum('INHERITANCE','INITIAL_ASSIGNMENT','PURCHASE','TRANSFER') DEFAULT NULL,
  `acquisition_price` decimal(10,2) DEFAULT NULL,
  `sale_price` decimal(10,2) DEFAULT NULL,
  `transferred_to_driver_id` bigint DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shift_owner` (`shift_id`, `owner_id`),
  KEY `idx_ownership_dates` (`shift_id`, `start_date`, `end_date`),
  CONSTRAINT `fk_ownership_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`),
  CONSTRAINT `fk_ownership_owner` FOREIGN KEY (`owner_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_ownership_transferred` FOREIGN KEY (`transferred_to_driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_id` bigint NOT NULL,
  `shift_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `log_date` date NOT NULL,
  `start_meter_reading` decimal(10,2) DEFAULT NULL,
  `end_meter_reading` decimal(10,2) DEFAULT NULL,
  `total_miles` decimal(10,2) DEFAULT NULL,
  `total_revenue` decimal(10,2) DEFAULT NULL,
  `total_expenses` decimal(10,2) DEFAULT NULL,
  `total_lease_amount` decimal(10,2) DEFAULT NULL,
  `lease_base_rate` decimal(10,2) DEFAULT NULL,
  `lease_mileage_rate` decimal(10,4) DEFAULT NULL,
  `lease_mileage_charge` decimal(10,2) DEFAULT NULL,
  `lease_total_amount` decimal(10,2) DEFAULT NULL,
  `net_revenue` decimal(10,2) DEFAULT NULL,
  `driver_earnings` decimal(10,2) DEFAULT NULL,
  `owner_earnings` decimal(10,2) DEFAULT NULL,
  `airport_trip_count` int NOT NULL DEFAULT 0,
  `settlement_status` enum('DISPUTED','PENDING','SETTLED') NOT NULL DEFAULT 'PENDING',
  `settled_at` datetime(6) DEFAULT NULL,
  `lease_plan_snapshot` varchar(200) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_shift_log` (`cab_id`, `shift_id`, `log_date`),
  KEY `idx_log_date` (`log_date`),
  KEY `idx_owner_date` (`owner_id`, `log_date`),
  KEY `idx_settlement` (`settlement_status`, `log_date`),
  CONSTRAINT `fk_shift_log_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_shift_log_owner` FOREIGN KEY (`owner_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_shift_log_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `driver_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift_log_id` bigint NOT NULL,
  `driver_id` bigint NOT NULL,
  `sequence_number` int NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `start_meter_reading` decimal(10,2) DEFAULT NULL,
  `end_meter_reading` decimal(10,2) DEFAULT NULL,
  `segment_miles` decimal(10,2) DEFAULT NULL,
  `segment_revenue` decimal(10,2) DEFAULT NULL,
  `segment_expenses` decimal(10,2) DEFAULT NULL,
  `segment_lease_share` decimal(10,2) DEFAULT NULL,
  `segment_net_earnings` decimal(10,2) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_segment_log` (`shift_log_id`, `sequence_number`),
  KEY `idx_segment_driver` (`driver_id`, `start_time`),
  CONSTRAINT `fk_segment_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_segment_log` FOREIGN KEY (`shift_log_id`) REFERENCES `shift_log` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `revenue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift_log_id` bigint NOT NULL,
  `segment_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `revenue_type` enum('AIRPORT_PICKUP','CHARGE_ACCOUNT','CREDIT_CARD','EXTRA_PASSENGER','LUGGAGE_FEE','OTHER','TIP','TOLL_REIMBURSEMENT','TRIP_FARE','WAITING_CHARGE') NOT NULL,
  `payment_method` enum('CASH','CHARGE_ACCOUNT','CHECK','CREDIT_CARD','DEBIT_CARD','DIGITAL_WALLET','MOBILE_APP','OTHER') NOT NULL,
  `customer_account` varchar(50) DEFAULT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_revenue_log` (`shift_log_id`, `timestamp`),
  KEY `idx_revenue_type` (`revenue_type`, `timestamp`),
  KEY `idx_revenue_payment` (`payment_method`, `reference_number`),
  CONSTRAINT `fk_revenue_log` FOREIGN KEY (`shift_log_id`) REFERENCES `shift_log` (`id`),
  CONSTRAINT `fk_revenue_segment` FOREIGN KEY (`segment_id`) REFERENCES `driver_segment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shift_expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift_log_id` bigint NOT NULL,
  `segment_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `expense_type` enum('ACCIDENT_DAMAGE','CAR_WASH','CLEANING','DISPATCH_FEE','FUEL','INSPECTION','INSURANCE','INTERNET_CHARGE','LICENSE_RENEWAL','MAINTENANCE','OIL_CHANGE','OTHER','PARKING','REGISTRATION','TICKET_FINE','TIRE_REPLACEMENT','TOLL') NOT NULL,
  `paid_by` enum('COMPANY','DRIVER','OWNER') NOT NULL,
  `responsible_party` enum('COMPANY','DRIVER','OWNER','SHARED') DEFAULT NULL,
  `vendor` varchar(100) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `receipt_url` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_expense_log` (`shift_log_id`, `timestamp`),
  KEY `idx_expense_type` (`expense_type`, `timestamp`),
  KEY `idx_expense_paid_by` (`paid_by`, `timestamp`),
  CONSTRAINT `fk_shift_expense_log` FOREIGN KEY (`shift_log_id`) REFERENCES `shift_log` (`id`),
  CONSTRAINT `fk_shift_expense_segment` FOREIGN KEY (`segment_id`) REFERENCES `driver_segment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `expense_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(50) NOT NULL,
  `category_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `category_type` enum('FIXED','VARIABLE') NOT NULL,
  `applies_to` enum('CAB','COMPANY','DRIVER','OWNER','SHIFT') NOT NULL,
  `application_type` VARCHAR(30) NOT NULL DEFAULT 'ALL_ACTIVE_SHIFTS',
  `is_active` bit DEFAULT b'1',
  `shift_profile_id` bigint DEFAULT NULL,
  `specific_shift_id` bigint DEFAULT NULL,
  `specific_driver_id` bigint DEFAULT NULL,
  `specific_owner_id` bigint DEFAULT NULL,
  `specific_person_id` bigint DEFAULT NULL,
  `attribute_type_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_expense_category_code` (`category_code`),
  KEY `idx_category_type` (`category_type`),
  KEY `idx_category_active` (`is_active`),
  CONSTRAINT `fk_expense_cat_driver` FOREIGN KEY (`specific_driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_expense_cat_owner` FOREIGN KEY (`specific_owner_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_expense_cat_shift` FOREIGN KEY (`specific_shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `expense_category_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expense_category_id` bigint NOT NULL,
  `configuration_mode` enum('AUTO_MATCH','INDIVIDUAL_CONFIG') NOT NULL,
  `matching_criteria` json DEFAULT NULL,
  `has_cab_type_rule` bit DEFAULT NULL,
  `has_share_type_rule` bit DEFAULT NULL,
  `has_airport_license_rule` bit DEFAULT NULL,
  `has_cab_shift_type_rule` bit DEFAULT NULL,
  `is_active` bit DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_expense_category_rule` (`expense_category_id`),
  KEY `idx_ecr_category` (`expense_category_id`),
  KEY `idx_ecr_mode` (`configuration_mode`),
  KEY `idx_ecr_share_rule` (`has_share_type_rule`),
  KEY `idx_ecr_airport_rule` (`has_airport_license_rule`),
  CONSTRAINT `fk_ecr_category` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `revenue_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(50) NOT NULL,
  `category_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `category_type` enum('FIXED','VARIABLE') NOT NULL,
  `applies_to` enum('CAB','COMPANY','DRIVER','OWNER','SHIFT') NOT NULL,
  `application_type` VARCHAR(30) NOT NULL DEFAULT 'ALL_ACTIVE_SHIFTS',
  `is_active` bit DEFAULT b'1',
  `shift_profile_id` bigint DEFAULT NULL,
  `specific_shift_id` bigint DEFAULT NULL,
  `specific_driver_id` bigint DEFAULT NULL,
  `specific_person_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_revenue_category_code` (`category_code`),
  KEY `idx_revenue_category_type` (`category_type`),
  KEY `idx_revenue_category_active` (`is_active`),
  KEY `idx_revenue_category_applies_to` (`applies_to`),
  CONSTRAINT `fk_revenue_cat_driver` FOREIGN KEY (`specific_driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_revenue_cat_person` FOREIGN KEY (`specific_person_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_revenue_cat_shift` FOREIGN KEY (`specific_shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tax_category_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tax_type_id` bigint NOT NULL,
  `expense_category_id` bigint NOT NULL,
  `assigned_at` date NOT NULL,
  `unassigned_at` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tca_tax_type` (`tax_type_id`),
  KEY `idx_tca_expense_cat` (`expense_category_id`),
  KEY `idx_tca_active` (`is_active`),
  CONSTRAINT `fk_tca_expense_cat` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_category` (`id`),
  CONSTRAINT `fk_tca_tax_type` FOREIGN KEY (`tax_type_id`) REFERENCES `tax_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `commission_category_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `commission_type_id` bigint NOT NULL,
  `revenue_category_id` bigint NOT NULL,
  `assigned_at` date NOT NULL,
  `unassigned_at` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cca_commission_type` (`commission_type_id`),
  KEY `idx_cca_revenue_cat` (`revenue_category_id`),
  KEY `idx_cca_active` (`is_active`),
  CONSTRAINT `fk_cca_commission_type` FOREIGN KEY (`commission_type_id`) REFERENCES `commission_type` (`id`),
  CONSTRAINT `fk_cca_revenue_cat` FOREIGN KEY (`revenue_category_id`) REFERENCES `revenue_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `account_id` varchar(50) NOT NULL,
  `invoice_number` varchar(50) NOT NULL,
  `invoice_date` date NOT NULL,
  `due_date` date NOT NULL,
  `billing_period_start` date NOT NULL,
  `billing_period_end` date NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `tax_rate` decimal(5,2) DEFAULT NULL,
  `tax_amount` decimal(10,2) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `amount_paid` decimal(10,2) NOT NULL DEFAULT 0.00,
  `previous_balance` decimal(10,2) DEFAULT NULL,
  `balance_due` decimal(10,2) NOT NULL,
  `status` enum('ARCHIVED','CANCELLED','DRAFT','LOCKED','OVERDUE','PAID','PARTIAL','POSTED','SENT') NOT NULL DEFAULT 'DRAFT',
  `statement_version` int DEFAULT 1,
  `parent_statement_id` bigint DEFAULT NULL,
  `pdf_path` varchar(500) DEFAULT NULL,
  `pdf_generated_at` datetime(6) DEFAULT NULL,
  `line_items_json` JSON DEFAULT NULL,
  `posted_at` datetime(6) DEFAULT NULL,
  `posted_by` bigint DEFAULT NULL,
  `locked_at` datetime(6) DEFAULT NULL,
  `locked_by` bigint DEFAULT NULL,
  `terms` varchar(500) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `email_send_count` int DEFAULT NULL,
  `last_email_sent_at` datetime(6) DEFAULT NULL,
  `last_email_sent_to` varchar(255) DEFAULT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_invoice_number` (`invoice_number`),
  KEY `idx_invoice_status` (`status`),
  KEY `idx_invoice_period` (`billing_period_start`, `billing_period_end`),
  CONSTRAINT `fk_invoice_customer` FOREIGN KEY (`customer_id`) REFERENCES `account_customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `account_charge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `account_id` varchar(50) NOT NULL,
  `trip_date` date NOT NULL,
  `start_time` time(6) DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `fare_amount` decimal(10,2) NOT NULL,
  `tip_amount` decimal(10,2) DEFAULT NULL,
  `is_paid` bit NOT NULL DEFAULT b'0',
  `paid_date` date DEFAULT NULL,
  `cab_id` bigint DEFAULT NULL,
  `driver_id` bigint DEFAULT NULL,
  `invoice_id` bigint DEFAULT NULL,
  `job_code` varchar(50) DEFAULT NULL,
  `invoice_number` varchar(50) DEFAULT NULL,
  `sub_account` varchar(50) DEFAULT NULL,
  `passenger_name` varchar(100) DEFAULT NULL,
  `pickup_address` varchar(300) DEFAULT NULL,
  `dropoff_address` varchar(300) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_charge_trip` (`account_id`, `cab_id`, `driver_id`, `trip_date`, `start_time`, `job_code`),
  KEY `idx_charge_customer` (`customer_id`),
  KEY `idx_charge_date` (`trip_date`),
  KEY `idx_charge_job_code` (`job_code`),
  KEY `idx_charge_paid` (`is_paid`),
  KEY `idx_charge_cab` (`cab_id`),
  KEY `idx_charge_driver` (`driver_id`),
  KEY `idx_charge_account_id` (`account_id`),
  KEY `idx_charge_invoice` (`invoice_id`),
  CONSTRAINT `fk_charge_customer` FOREIGN KEY (`customer_id`) REFERENCES `account_customer` (`id`),
  CONSTRAINT `fk_charge_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_charge_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `account_credit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `account_id` varchar(50) NOT NULL,
  `credit_amount` decimal(38,2) NOT NULL,
  `used_amount` decimal(38,2) DEFAULT NULL,
  `remaining_amount` decimal(38,2) NOT NULL,
  `is_active` bit DEFAULT b'1',
  `source_type` varchar(50) NOT NULL,
  `source_reference` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `payment_id` bigint DEFAULT NULL,
  `invoice_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_date` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_credit_customer` (`customer_id`),
  KEY `idx_credit_account` (`account_id`),
  KEY `idx_credit_date` (`created_date`),
  CONSTRAINT `fk_credit_customer` FOREIGN KEY (`customer_id`) REFERENCES `account_customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `invoice_line_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  `charge_id` bigint NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `trip_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_line_item_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`id`),
  CONSTRAINT `fk_line_item_charge` FOREIGN KEY (`charge_id`) REFERENCES `account_charge` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lease_expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `driver_id` bigint NOT NULL,
  `cab_id` bigint NOT NULL,
  `shift_id` bigint DEFAULT NULL,
  `plan_id` bigint NOT NULL,
  `rate_id` bigint NOT NULL,
  `lease_date` date NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,
  `shift_type` enum('DAY','NIGHT') NOT NULL,
  `cab_type` enum('HANDICAP_VAN','SEDAN') NOT NULL,
  `has_airport_license` bit NOT NULL DEFAULT b'0',
  `base_amount` decimal(10,2) NOT NULL,
  `miles_driven` decimal(10,2) DEFAULT NULL,
  `mileage_amount` decimal(10,2) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `is_paid` bit DEFAULT NULL,
  `paid_date` date DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_lease_expense_plan` FOREIGN KEY (`plan_id`) REFERENCES `lease_plan` (`id`),
  CONSTRAINT `fk_lease_expense_rate` FOREIGN KEY (`rate_id`) REFERENCES `lease_rate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lease_rate_overrides` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_driver_number` varchar(50) NOT NULL,
  `cab_number` varchar(50) DEFAULT NULL,
  `shift_type` varchar(20) DEFAULT NULL,
  `day_of_week` varchar(20) DEFAULT NULL,
  `lease_rate` decimal(10,2) NOT NULL,
  `priority` int NOT NULL DEFAULT 1,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_owner_cab` (`owner_driver_number`, `cab_number`),
  KEY `idx_dates` (`start_date`, `end_date`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `item_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `unit_type` enum('AIRPORT_TRIP','INSURANCE','MILEAGE') NOT NULL,
  `charged_to` enum('DRIVER','OWNER') NOT NULL,
  `rate` decimal(10,4) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `attribute_type_id` bigint DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_item_rate_active` (`is_active`),
  KEY `idx_item_rate_unit_type` (`unit_type`),
  KEY `idx_item_rate_effective_date` (`effective_from`, `effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `item_rate_override` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_rate_id` bigint NOT NULL,
  `owner_driver_number` varchar(50) NOT NULL,
  `cab_number` varchar(50) DEFAULT NULL,
  `shift_type` varchar(20) DEFAULT NULL,
  `day_of_week` varchar(20) DEFAULT NULL,
  `override_rate` decimal(10,4) NOT NULL,
  `priority` int NOT NULL DEFAULT 1,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `is_active` bit NOT NULL DEFAULT b'1',
  `notes` varchar(500) DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_item_rate_override_item` (`item_rate_id`),
  KEY `idx_item_rate_override_owner_cab` (`owner_driver_number`, `cab_number`),
  KEY `idx_item_rate_override_dates` (`start_date`, `end_date`),
  KEY `idx_item_rate_override_active` (`is_active`),
  CONSTRAINT `fk_item_rate_override_item` FOREIGN KEY (`item_rate_id`) REFERENCES `item_rate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `one_time_expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL DEFAULT 'Unnamed Expense',
  `expense_date` date NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `paid_by` enum('COMPANY','DRIVER','OWNER','THIRD_PARTY') NOT NULL,
  `responsible_party` enum('COMPANY','DRIVER','OWNER','SHARED') DEFAULT NULL,
  `entity_type` enum('CAB','COMPANY','DRIVER','OWNER','SHIFT') DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `expense_category_id` bigint DEFAULT NULL,
  `application_type` enum('ALL_ACTIVE_SHIFTS','ALL_DRIVERS','ALL_OWNERS','SHIFTS_WITH_ATTRIBUTE','SHIFT_PROFILE','SPECIFIC_PERSON','SPECIFIC_SHIFT') DEFAULT NULL,
  `shift_profile_id` bigint DEFAULT NULL,
  `specific_shift_id` bigint DEFAULT NULL,
  `specific_person_id` bigint DEFAULT NULL,
  `attribute_type_id` bigint DEFAULT NULL,
  `shift_type` enum('DAY','NIGHT') DEFAULT NULL,
  `cab_id` bigint DEFAULT NULL,
  `driver_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `is_reimbursable` bit DEFAULT NULL,
  `is_reimbursed` bit DEFAULT NULL,
  `reimbursed_date` date DEFAULT NULL,
  `invoice_number` varchar(100) DEFAULT NULL,
  `vendor` varchar(200) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `receipt_url` varchar(500) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_onetime_category` (`expense_category_id`),
  KEY `idx_onetime_entity` (`entity_type`, `entity_id`),
  KEY `idx_onetime_date` (`expense_date`),
  KEY `idx_onetime_paid_by` (`paid_by`),
  KEY `idx_onetime_shift_type` (`shift_type`),
  CONSTRAINT `fk_onetime_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_onetime_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_onetime_category` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_category` (`id`),
  CONSTRAINT `fk_onetime_owner` FOREIGN KEY (`owner_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recurring_expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expense_category_id` bigint DEFAULT NULL,
  `entity_type` enum('CAB','COMPANY','DRIVER','OWNER','SHIFT') DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `billing_method` enum('DAILY','MONTHLY','PER_SHIFT') NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `is_active` bit DEFAULT b'1',
  `shift_type` enum('DAY','NIGHT') DEFAULT NULL,
  `application_type_enum` varchar(30) DEFAULT NULL,
  `shift_profile_id` bigint DEFAULT NULL,
  `specific_shift_id` bigint DEFAULT NULL,
  `specific_person_id` bigint DEFAULT NULL,
  `attribute_type_id` bigint DEFAULT NULL,
  `shift_id` bigint DEFAULT NULL,
  `attribute_cost_id` bigint DEFAULT NULL,
  `cab_id` bigint DEFAULT NULL,
  `driver_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `is_auto_generated` bit DEFAULT NULL,
  `source_rule_id` bigint DEFAULT NULL,
  `generation_date` datetime(6) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recurring_shift_attribute` (`shift_id`, `attribute_cost_id`),
  KEY `idx_recurring_category` (`expense_category_id`),
  KEY `idx_recurring_entity` (`entity_type`, `entity_id`),
  KEY `idx_recurring_dates` (`effective_from`, `effective_to`),
  KEY `idx_recurring_active` (`is_active`),
  KEY `idx_recurring_shift_type` (`shift_type`),
  CONSTRAINT `fk_recurring_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_recurring_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_recurring_category` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_category` (`id`),
  CONSTRAINT `fk_recurring_owner` FOREIGN KEY (`owner_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_recurring_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recurring_expense_auto_creation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expense_category_rule_id` bigint NOT NULL,
  `recurring_expense_id` bigint NOT NULL,
  `creation_type` enum('AUTO_MATCHED','BULK_CONFIGURED','MANUAL') NOT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `matching_snapshot` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reac_rule` (`expense_category_rule_id`),
  KEY `idx_reac_expense` (`recurring_expense_id`),
  KEY `idx_reac_type` (`creation_type`),
  KEY `idx_reac_created` (`created_at`),
  CONSTRAINT `fk_reac_rule` FOREIGN KEY (`expense_category_rule_id`) REFERENCES `expense_category_rule` (`id`),
  CONSTRAINT `fk_reac_expense` FOREIGN KEY (`recurring_expense_id`) REFERENCES `recurring_expense` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `other_revenue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `revenue_category_id` bigint NOT NULL,
  `entity_type` enum('CAB','COMPANY','DRIVER','OWNER','SHIFT') NOT NULL,
  `entity_id` bigint NOT NULL,
  `revenue_type` enum('ADJUSTMENT','BONUS','COMMISSION','CREDIT','INCENTIVE','OTHER','REFERRAL','REFUND','REIMBURSEMENT') NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `revenue_date` date NOT NULL,
  `payment_date` date DEFAULT NULL,
  `payment_status` enum('CANCELLED','PAID','PENDING','PROCESSING') NOT NULL DEFAULT 'PENDING',
  `payment_method` varchar(100) DEFAULT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `application_type` enum('ALL_ACTIVE_SHIFTS','ALL_DRIVERS','ALL_OWNERS','SHIFTS_WITH_ATTRIBUTE','SHIFT_PROFILE','SPECIFIC_PERSON','SPECIFIC_SHIFT') DEFAULT NULL,
  `shift_profile_id` bigint DEFAULT NULL,
  `specific_shift_id` bigint DEFAULT NULL,
  `specific_person_id` bigint DEFAULT NULL,
  `attribute_type_id` bigint DEFAULT NULL,
  `cab_id` bigint DEFAULT NULL,
  `driver_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `shift_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_revenue_category` (`revenue_category_id`),
  KEY `idx_revenue_entity` (`entity_type`, `entity_id`),
  KEY `idx_revenue_date` (`revenue_date`),
  KEY `idx_revenue_type` (`revenue_type`),
  KEY `idx_revenue_payment_status` (`payment_status`),
  KEY `idx_other_revenue_app_type` (`application_type`),
  KEY `idx_other_revenue_shift_profile` (`shift_profile_id`),
  KEY `idx_other_revenue_specific_shift` (`specific_shift_id`),
  KEY `idx_other_revenue_specific_person` (`specific_person_id`),
  KEY `idx_other_revenue_attribute_type` (`attribute_type_id`),
  KEY `idx_other_revenue_app_type_date` (`application_type`, `revenue_date`),
  CONSTRAINT `fk_other_revenue_rev_cat` FOREIGN KEY (`revenue_category_id`) REFERENCES `revenue_category` (`id`),
  CONSTRAINT `fk_other_revenue_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`),
  CONSTRAINT `fk_other_revenue_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_other_revenue_owner` FOREIGN KEY (`owner_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_other_revenue_shift` FOREIGN KEY (`shift_id`) REFERENCES `cab_shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- SECTION 4: Operational Data Tables
-- ============================================================================

CREATE TABLE `driver_shifts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `driver_number` varchar(50) NOT NULL,
  `cab_number` varchar(50) NOT NULL,
  `logon_time` datetime(6) NOT NULL,
  `logoff_time` datetime(6) DEFAULT NULL,
  `total_revenue` decimal(10,2) DEFAULT NULL,
  `total_trips` int DEFAULT NULL,
  `total_distance` decimal(10,2) DEFAULT NULL,
  `total_hours` decimal(5,2) DEFAULT NULL,
  `primary_shift_type` varchar(10) DEFAULT NULL,
  `primary_shift_count` decimal(3,2) DEFAULT NULL,
  `secondary_shift_type` varchar(10) DEFAULT NULL,
  `secondary_shift_count` decimal(3,2) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `driver_username` varchar(50) DEFAULT NULL,
  `driver_first_name` varchar(100) DEFAULT NULL,
  `driver_last_name` varchar(100) DEFAULT NULL,
  `session_count` int DEFAULT NULL,
  `session_history` JSON DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `airport_trips` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_number` varchar(50) NOT NULL,
  `driver_number` varchar(50) DEFAULT NULL,
  `vehicle_name` varchar(100) DEFAULT NULL,
  `trip_date` date NOT NULL,
  `shift` varchar(10) NOT NULL DEFAULT 'BOTH',
  `grand_total` int NOT NULL DEFAULT 0,
  `hour_00` int DEFAULT NULL, `hour_01` int DEFAULT NULL, `hour_02` int DEFAULT NULL,
  `hour_03` int DEFAULT NULL, `hour_04` int DEFAULT NULL, `hour_05` int DEFAULT NULL,
  `hour_06` int DEFAULT NULL, `hour_07` int DEFAULT NULL, `hour_08` int DEFAULT NULL,
  `hour_09` int DEFAULT NULL, `hour_10` int DEFAULT NULL, `hour_11` int DEFAULT NULL,
  `hour_12` int DEFAULT NULL, `hour_13` int DEFAULT NULL, `hour_14` int DEFAULT NULL,
  `hour_15` int DEFAULT NULL, `hour_16` int DEFAULT NULL, `hour_17` int DEFAULT NULL,
  `hour_18` int DEFAULT NULL, `hour_19` int DEFAULT NULL, `hour_20` int DEFAULT NULL,
  `hour_21` int DEFAULT NULL, `hour_22` int DEFAULT NULL, `hour_23` int DEFAULT NULL,
  `upload_batch_id` varchar(100) DEFAULT NULL,
  `uploaded_by` varchar(100) DEFAULT NULL,
  `upload_filename` varchar(255) DEFAULT NULL,
  `upload_date` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_airport_cab_date` (`cab_number`, `trip_date`),
  KEY `idx_airport_cab_number` (`cab_number`),
  KEY `idx_airport_trip_date` (`trip_date`),
  KEY `idx_airport_upload_batch` (`upload_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `airport_trip_driver` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `airport_trip_id` bigint NOT NULL,
  `cab_number` varchar(50) NOT NULL,
  `driver_number` varchar(50) NOT NULL,
  `trip_date` date NOT NULL,
  `hour` int NOT NULL,
  `trip_count` int NOT NULL DEFAULT 0,
  `total_daily_trips` int DEFAULT NULL,
  `assignment_method` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_atd_airport_trip` (`airport_trip_id`),
  KEY `idx_atd_driver_number` (`driver_number`),
  KEY `idx_atd_trip_date` (`trip_date`),
  KEY `idx_atd_driver_date` (`driver_number`, `trip_date`),
  CONSTRAINT `fk_atd_airport_trip` FOREIGN KEY (`airport_trip_id`) REFERENCES `airport_trips` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mileage_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_number` varchar(50) NOT NULL,
  `driver_number` varchar(50) DEFAULT NULL,
  `logon_time` datetime(6) NOT NULL,
  `logoff_time` datetime(6) DEFAULT NULL,
  `mileage_a` decimal(10,3) DEFAULT NULL,
  `mileage_b` decimal(10,3) DEFAULT NULL,
  `mileage_c` decimal(10,3) DEFAULT NULL,
  `total_mileage` decimal(10,3) DEFAULT NULL,
  `shift_hours` decimal(5,2) DEFAULT NULL,
  `upload_batch_id` varchar(100) DEFAULT NULL,
  `uploaded_by` varchar(100) DEFAULT NULL,
  `upload_filename` varchar(255) DEFAULT NULL,
  `upload_date` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mileage_cab_number` (`cab_number`),
  KEY `idx_mileage_driver_number` (`driver_number`),
  KEY `idx_mileage_logon_time` (`logon_time`),
  KEY `idx_mileage_upload_batch` (`upload_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `merchant2cab` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_number` varchar(100) NOT NULL,
  `cab_number` varchar(20) NOT NULL,
  `shift` varchar(10) NOT NULL DEFAULT 'BOTH',
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cab_merchant_active` (`cab_number`, `merchant_number`, `start_date`, `shift`),
  KEY `idx_cab_number` (`cab_number`),
  KEY `idx_merchant_number` (`merchant_number`),
  KEY `idx_active_mappings` (`cab_number`, `end_date`),
  KEY `idx_cab_shift` (`cab_number`, `shift`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `credit_card_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_id` varchar(100) DEFAULT NULL,
  `authorization_code` varchar(50) NOT NULL,
  `merchant_id` varchar(50) NOT NULL,
  `terminal_id` varchar(50) NOT NULL,
  `transaction_date` date NOT NULL,
  `transaction_time` time(6) NOT NULL,
  `settlement_date` date DEFAULT NULL,
  `card_type` varchar(20) DEFAULT NULL,
  `card_last_four` varchar(4) DEFAULT NULL,
  `card_brand` varchar(30) DEFAULT NULL,
  `cardholder_number` varchar(30) DEFAULT NULL,
  `cab_number` varchar(20) DEFAULT NULL,
  `driver_number` varchar(20) DEFAULT NULL,
  `job_id` varchar(50) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `tip_amount` decimal(10,2) DEFAULT NULL,
  `processing_fee` decimal(10,2) DEFAULT NULL,
  `transaction_status` enum('DECLINED','DISPUTED','PENDING','REFUNDED','SETTLED') NOT NULL DEFAULT 'PENDING',
  `is_settled` bit NOT NULL DEFAULT b'0',
  `is_refunded` bit NOT NULL DEFAULT b'0',
  `refund_amount` decimal(10,2) DEFAULT NULL,
  `refund_date` date DEFAULT NULL,
  `batch_number` varchar(50) DEFAULT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `capture_method` varchar(5) DEFAULT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `receipt_number` varchar(50) DEFAULT NULL,
  `upload_batch_id` varchar(50) DEFAULT NULL,
  `upload_filename` varchar(255) DEFAULT NULL,
  `upload_date` datetime(6) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cc_transaction_natural` (`merchant_id`, `terminal_id`, `authorization_code`, `transaction_date`, `transaction_time`),
  KEY `idx_cc_transaction_date` (`transaction_date`),
  KEY `idx_cc_settlement_date` (`settlement_date`),
  KEY `idx_cc_driver_number` (`driver_number`),
  KEY `idx_cc_cab_number` (`cab_number`),
  KEY `idx_cc_status` (`transaction_status`),
  KEY `idx_cc_settled` (`is_settled`),
  KEY `idx_cc_merchant_terminal` (`merchant_id`, `terminal_id`),
  KEY `idx_cc_upload_batch` (`upload_batch_id`),
  KEY `idx_cc_auth_code` (`authorization_code`),
  KEY `idx_cc_job_id` (`job_id`),
  KEY `idx_cc_cardholder_number` (`cardholder_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `credit_card_upload_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` varchar(50) NOT NULL,
  `filename` varchar(255) NOT NULL,
  `upload_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uploaded_by` bigint DEFAULT NULL,
  `total_rows` int NOT NULL DEFAULT 0,
  `processed_rows` int NOT NULL DEFAULT 0,
  `duplicate_rows` int NOT NULL DEFAULT 0,
  `error_rows` int NOT NULL DEFAULT 0,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'PROCESSING',
  `error_log` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_id` (`batch_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_upload_date` (`upload_date`),
  KEY `idx_uploaded_by` (`uploaded_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `driver_trips` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `driver_id` bigint DEFAULT NULL,
  `cab_id` bigint DEFAULT NULL,
  `driver_username` varchar(50) DEFAULT NULL,
  `driver_name` varchar(200) DEFAULT NULL,
  `company_id` varchar(50) DEFAULT NULL,
  `account_number` varchar(100) DEFAULT NULL,
  `job_code` varchar(50) DEFAULT NULL,
  `trip_date` date NOT NULL,
  `start_time` time(6) DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `fare_amount` decimal(10,2) DEFAULT NULL,
  `tip_amount` decimal(10,2) DEFAULT NULL,
  `passenger_name` varchar(200) DEFAULT NULL,
  `pickup_address` varchar(500) DEFAULT NULL,
  `dropoff_address` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_driver_trip_job` (`job_code`, `driver_id`, `cab_id`, `trip_date`),
  KEY `idx_dtrip_driver` (`driver_id`),
  KEY `idx_dtrip_cab` (`cab_id`),
  KEY `idx_dtrip_date` (`trip_date`),
  KEY `idx_dtrip_job_code` (`job_code`),
  KEY `idx_dtrip_driver_username` (`driver_username`),
  CONSTRAINT `fk_dtrip_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_dtrip_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- SECTION 5: Payment & Statement Tables
-- ============================================================================

CREATE TABLE `payment_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_number` varchar(50) NOT NULL,
  `batch_name` varchar(255) DEFAULT NULL,
  `batch_date` date NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `payment_count` int NOT NULL DEFAULT 0,
  `actual_payments_made` decimal(10,2) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `statement_ids` TEXT DEFAULT NULL,
  `reconciliation_notes` TEXT DEFAULT NULL,
  `posted_at` datetime(6) DEFAULT NULL,
  `posted_by` bigint DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `processed_by` bigint DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_batch_number` (`batch_number`),
  KEY `idx_batch_date` (`batch_date`),
  KEY `idx_batch_status` (`status`),
  KEY `idx_batch_period` (`period_start`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_id` bigint NOT NULL,
  `payment_batch_id` bigint DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `payment_method_id` bigint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `payment_date` date NOT NULL,
  `payment_number` varchar(50) NOT NULL,
  `payment_method` varchar(50) DEFAULT NULL,
  `account_id` varchar(50) NOT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `notes` TEXT DEFAULT NULL,
  `posted_at` datetime(6) DEFAULT NULL,
  `posted_by` bigint DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_payment_number` (`payment_number`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_payment_status` (`status`),
  KEY `idx_payment_statement` (`statement_id`),
  CONSTRAINT `fk_payment_batch` FOREIGN KEY (`payment_batch_id`) REFERENCES `payment_batch` (`id`),
  CONSTRAINT `fk_payment_method` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_method` (`id`),
  CONSTRAINT `fk_payment_statement` FOREIGN KEY (`statement_id`) REFERENCES `invoice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `statement_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_id` bigint NOT NULL,
  `payment_batch_id` bigint DEFAULT NULL,
  `payment_method_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  `person_type` varchar(20) NOT NULL,
  `person_name` varchar(255) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `payment_date` date NOT NULL,
  `payment_number` varchar(50) NOT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `notes` TEXT DEFAULT NULL,
  `posted_at` datetime(6) DEFAULT NULL,
  `posted_by` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_stmt_payment_number` (`payment_number`),
  CONSTRAINT `fk_stmt_payment_batch` FOREIGN KEY (`payment_batch_id`) REFERENCES `payment_batch` (`id`),
  CONSTRAINT `fk_stmt_payment_method` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_method` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `statements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_id` bigint DEFAULT NULL,
  `person_type` varchar(255) DEFAULT NULL,
  `person_name` varchar(255) DEFAULT NULL,
  `period_from` date DEFAULT NULL,
  `period_to` date DEFAULT NULL,
  `total_revenues` decimal(38,2) DEFAULT NULL,
  `total_expenses` decimal(38,2) DEFAULT NULL,
  `total_recurring_expenses` decimal(38,2) DEFAULT NULL,
  `total_one_time_expenses` decimal(38,2) DEFAULT NULL,
  `previous_balance` decimal(38,2) DEFAULT NULL,
  `net_due` decimal(38,2) DEFAULT NULL,
  `paid_amount` decimal(38,2) DEFAULT NULL,
  `status` enum('DRAFT','FINALIZED','PAID') DEFAULT 'DRAFT',
  `line_items_json` LONGTEXT DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `generated_date` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `statement_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_id` bigint NOT NULL,
  `change_type` varchar(50) NOT NULL,
  `previous_status` varchar(20) DEFAULT NULL,
  `new_status` varchar(20) DEFAULT NULL,
  `change_description` TEXT DEFAULT NULL,
  `changed_fields` JSON DEFAULT NULL,
  `changed_by` bigint NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `reason` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_statement` (`statement_id`),
  KEY `idx_audit_change_type` (`change_type`),
  KEY `idx_audit_changed_at` (`changed_at`),
  CONSTRAINT `fk_audit_statement` FOREIGN KEY (`statement_id`) REFERENCES `invoice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `eft_file_generation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `file_creation_number` int NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `record_count` int NOT NULL DEFAULT 0,
  `total_credit_amount` decimal(14,2) NOT NULL DEFAULT 0.00,
  `total_debit_amount` decimal(14,2) NOT NULL DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'GENERATED',
  `generated_by` varchar(100) DEFAULT NULL,
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submitted_at` datetime DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_eft_file_batch` (`batch_id`),
  KEY `idx_eft_file_status` (`status`),
  CONSTRAINT `fk_eft_file_batch` FOREIGN KEY (`batch_id`) REFERENCES `payment_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- SECTION 6: Seed Data
-- ============================================================================

-- Default cab attribute types
INSERT INTO `cab_attribute_type` (`attribute_code`, `attribute_name`, `description`, `category`, `data_type`, `requires_value`, `is_active`, `created_at`)
VALUES
  ('AIRPORT_LICENSE', 'Airport License', 'License to operate at airports', 'LICENSE', 'STRING', 1, 1, NOW()),
  ('TRANSPONDER', 'Transponder', 'Electronic transponder for tracking', 'EQUIPMENT', 'STRING', 1, 1, NOW()),
  ('VEHICLE_TYPE_VAN', 'Van Classification', 'Vehicle classified as van', 'TYPE', 'BOOLEAN', 0, 1, NOW()),
  ('VEHICLE_TYPE_SEDAN', 'Sedan Classification', 'Vehicle classified as sedan', 'TYPE', 'BOOLEAN', 0, 1, NOW()),
  ('HANDICAP_ACCESSIBLE', 'Handicap Accessible', 'Vehicle is handicap accessible', 'TYPE', 'BOOLEAN', 0, 1, NOW());

-- Default shift profiles
INSERT INTO `shift_profile` (
  `profile_code`, `profile_name`, `description`,
  `cab_type`, `share_type`, `has_airport_license`, `shift_type`,
  `category`, `color_code`, `display_order`,
  `is_active`, `is_system_profile`, `usage_count`, `created_by`, `created_at`
) VALUES
  ('STANDARD_SEDAN_VOTING', 'Standard Sedan - Voting Share', 'Regular sedan taxi with voting share rights',
   'SEDAN', 'VOTING_SHARE', 0, NULL, 'STANDARD', '#3E5244', 1, 1, 1, 0, 'SYSTEM', NOW()),
  ('STANDARD_SEDAN_NON_VOTING', 'Standard Sedan - Non-Voting Share', 'Regular sedan taxi with non-voting share rights',
   'SEDAN', 'NON_VOTING_SHARE', 0, NULL, 'STANDARD', '#5B6B68', 2, 1, 1, 0, 'SYSTEM', NOW()),
  ('PREMIUM_SEDAN_VOTING', 'Premium Sedan - Airport License - Voting', 'Premium sedan with airport license and voting share rights',
   'SEDAN', 'VOTING_SHARE', 1, NULL, 'PREMIUM', '#10B981', 3, 1, 1, 0, 'SYSTEM', NOW()),
  ('PREMIUM_SEDAN_NON_VOTING', 'Premium Sedan - Airport License - Non-Voting', 'Premium sedan with airport license and non-voting share rights',
   'SEDAN', 'NON_VOTING_SHARE', 1, NULL, 'PREMIUM', '#059669', 4, 1, 1, 0, 'SYSTEM', NOW()),
  ('HANDICAP_VAN_VOTING', 'Handicap Van - Voting Share', 'Accessible handicap van with voting share rights',
   'HANDICAP_VAN', 'VOTING_SHARE', 0, NULL, 'SPECIAL', '#F59E0B', 5, 1, 1, 0, 'SYSTEM', NOW()),
  ('HANDICAP_VAN_NON_VOTING', 'Handicap Van - Non-Voting Share', 'Accessible handicap van with non-voting share rights',
   'HANDICAP_VAN', 'NON_VOTING_SHARE', 0, NULL, 'SPECIAL', '#DC2626', 6, 1, 1, 0, 'SYSTEM', NOW()),
  ('DAY_SHIFT_SEDAN', 'Day Shift - Sedan Operations', 'Sedan operations specifically for day shift (06:00-18:00)',
   'SEDAN', NULL, NULL, 'DAY', 'TIME_BASED', '#60A5FA', 7, 1, 1, 0, 'SYSTEM', NOW()),
  ('NIGHT_SHIFT_SEDAN', 'Night Shift - Sedan Operations', 'Sedan operations specifically for night shift (18:00-06:00)',
   'SEDAN', NULL, NULL, 'NIGHT', 'TIME_BASED', '#3B82F6', 8, 1, 1, 0, 'SYSTEM', NOW());

-- Default payment methods
INSERT INTO `payment_method` (`method_name`, `method_code`, `requires_reference`, `requires_bank_details`, `is_automatic`, `is_active`, `display_order`)
VALUES
  ('Cheque', 'CHQ', 1, 0, 0, 1, 1),
  ('Direct Deposit', 'DD', 1, 1, 0, 1, 2),
  ('Cash', 'CASH', 0, 0, 0, 1, 3),
  ('E-Transfer', 'ETRF', 1, 0, 0, 1, 4);

-- Taxable expense categories (for tax configuration only)
INSERT INTO `expense_category` (`category_code`, `category_name`, `description`, `category_type`, `applies_to`, `application_type`, `is_active`, `created_at`, `updated_at`)
VALUES
  ('AIRPORT_TRIP', 'Airport Trip Expense', 'Per-trip airport charges (transponder or airport plate)', 'VARIABLE', 'DRIVER', 'ALL_ACTIVE_SHIFTS', 1, NOW(), NOW()),
  ('INSURANCE_MILEAGE', 'Insurance Mileage', 'Per-mile insurance charges based on mileage records', 'VARIABLE', 'DRIVER', 'ALL_ACTIVE_SHIFTS', 1, NOW(), NOW());

-- Tenant config (UPDATE with actual TaxiCaller API values for new tenant)
INSERT INTO `tenant_config` (`tenant_id`, `company_name`, `taxicaller_api_key`, `taxicaller_company_id`, `created_at`)
VALUES ('fareflow_new', 'New Tenant Company', '', 0, NOW());

-- Default admin user
-- Password: Welcome1!  (BCrypt cost 10)
-- *** CHANGE THIS PASSWORD immediately after first login ***
INSERT INTO `user` (`username`, `password`, `first_name`, `last_name`, `email`, `role`, `is_active`, `created_at`, `updated_at`)
VALUES ('admin', '$2a$10$8OUV551g9GgzQ5Wbx0xgg.aZ40NAhjQNpvOp4eCP8fTpJ6ZbchU.m', 'Admin', 'User', NULL, 'ADMIN', 1, NOW(), NOW());

-- ============================================================================
-- DONE. Post-setup checklist:
--   1. Replace ALL occurrences of 'fareflow_new' with actual tenant schema name
--      (database name at top + tenant_config INSERT)
--   2. Update tenant_config with actual TaxiCaller API key and company ID
--   3. Login with:  username: admin  /  password: Welcome1!
--   4. CHANGE the admin password immediately after first login
--   5. Register the tenant in the shared tenant_registry table
--   6. Update application.yml or environment with the new tenant schema
-- ============================================================================
