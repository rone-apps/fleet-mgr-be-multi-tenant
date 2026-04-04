-- Year End Report configuration: controls which line items are visible
CREATE TABLE `year_end_report_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `section` varchar(20) NOT NULL COMMENT 'REVENUE, EXPENSE, TAX, COMMISSION, SUMMARY',
  `item_key` varchar(100) NOT NULL COMMENT 'Unique key matching breakdown item keys',
  `item_label` varchar(200) NOT NULL COMMENT 'Display label',
  `is_visible` tinyint(1) NOT NULL DEFAULT 1,
  `display_order` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_yer_section_key` (`section`, `item_key`),
  INDEX `idx_yer_section` (`section`),
  INDEX `idx_yer_visible` (`is_visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default config items
INSERT INTO `year_end_report_config` (`section`, `item_key`, `item_label`, `is_visible`, `display_order`, `created_at`) VALUES
-- Revenue items
('REVENUE', 'LEASE_REVENUE',        'Lease Revenue',              1, 10, NOW()),
('REVENUE', 'CREDIT_CARD_REVENUE',  'Credit Card Revenue',        1, 20, NOW()),
('REVENUE', 'CHARGES_REVENUE',      'Account Charges Revenue',    1, 30, NOW()),
('REVENUE', 'OTHER_REVENUE',        'Other Revenue',              1, 40, NOW()),
-- Expense items
('EXPENSE', 'LEASE_EXPENSE',        'Lease Expense',              1, 10, NOW()),
('EXPENSE', 'FIXED_EXPENSE',        'Fixed Expenses',             1, 20, NOW()),
('EXPENSE', 'VARIABLE_EXPENSE',     'Variable / One-Time Expenses', 1, 30, NOW()),
('EXPENSE', 'INSURANCE_MILEAGE',    'Insurance & Mileage',        1, 40, NOW()),
('EXPENSE', 'AIRPORT_TRIPS',        'Airport Trip Charges',       1, 50, NOW()),
-- Tax & Commission
('TAX',        'TAX_EXPENSE',        'Taxes (HST/GST)',           1, 10, NOW()),
('COMMISSION', 'COMMISSION_EXPENSE', 'Commissions',               1, 10, NOW()),
-- Summary
('SUMMARY', 'TOTAL_REVENUE',   'Total Revenue',        1, 10, NOW()),
('SUMMARY', 'TOTAL_EXPENSE',   'Total Expenses',       1, 20, NOW()),
('SUMMARY', 'NET_INCOME',      'Net Income',           1, 30, NOW()),
('SUMMARY', 'PREVIOUS_BALANCE','Previous Balance',     1, 40, NOW()),
('SUMMARY', 'PAYMENTS_MADE',   'Payments Made',        1, 50, NOW()),
('SUMMARY', 'OUTSTANDING',     'Outstanding Balance',  1, 60, NOW());
