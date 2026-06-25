-- Create legacy balance table for manual balance imports
-- This table stores manually imported balance data as an alternative to
-- automatic balance-forward from finalized statements (SmartFleets AI)

CREATE TABLE legacy_balance_owed (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    driver_number VARCHAR(50) NOT NULL COMMENT 'Business key for driver lookup',
    driver_name VARCHAR(255) COMMENT 'Driver full name for reference',
    is_owner TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=owner, 0=driver',
    balance_owed DECIMAL(19, 2) NOT NULL COMMENT 'Negative value = driver owes company',
    effective_date DATE NOT NULL COMMENT 'Date this balance became effective',
    source VARCHAR(50) DEFAULT 'CSV_IMPORT' COMMENT 'Data source identifier',
    notes TEXT COMMENT 'Additional context or remarks',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_driver_number (driver_number),
    INDEX idx_effective_date (effective_date),
    UNIQUE KEY uk_driver_date (driver_number, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Manual legacy balance entries for drivers/owners';

-- Rollback:
-- DROP TABLE IF EXISTS legacy_balance_owed;
