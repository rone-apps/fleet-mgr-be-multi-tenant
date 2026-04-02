-- EFT (Electronic Funds Transfer) infrastructure for CPA Standard 005 file generation

-- Company/originator EFT configuration (one row per tenant)
CREATE TABLE eft_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    originator_id VARCHAR(10) NOT NULL COMMENT 'CPA originator ID assigned by bank (10 digits)',
    originator_short_name VARCHAR(15) NOT NULL COMMENT 'Appears on payee bank statement',
    originator_long_name VARCHAR(30) NOT NULL COMMENT 'Full company name',
    processing_centre VARCHAR(5) DEFAULT '' COMMENT 'Bank-assigned data centre code',
    currency_code VARCHAR(3) NOT NULL DEFAULT 'CAD' COMMENT 'CAD or USD',
    transaction_code VARCHAR(3) NOT NULL DEFAULT '200' COMMENT 'CPA transaction code (200=Payroll)',
    return_institution_id VARCHAR(4) NOT NULL COMMENT '3-digit institution number of company bank',
    return_transit_number VARCHAR(5) NOT NULL COMMENT '5-digit transit/branch number',
    return_account_number VARCHAR(12) NOT NULL COMMENT 'Company bank account for returns',
    file_creation_number INT NOT NULL DEFAULT 1 COMMENT 'Sequential file number (1-9999)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_eft_config_originator (originator_id)
) COMMENT 'EFT originator configuration for CPA 005 file generation';

-- Bank account details for drivers/owners (linked to driver table)
CREATE TABLE bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL COMMENT 'FK to driver table',
    account_holder_name VARCHAR(30) NOT NULL COMMENT 'Name as it appears on the account',
    institution_number VARCHAR(4) NOT NULL COMMENT '3-digit bank institution number',
    transit_number VARCHAR(5) NOT NULL COMMENT '5-digit branch/transit number',
    account_number VARCHAR(12) NOT NULL COMMENT 'Bank account number',
    account_type VARCHAR(10) NOT NULL DEFAULT 'CHEQUING' COMMENT 'CHEQUING or SAVINGS',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Pre-note sent and confirmed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_account_driver FOREIGN KEY (driver_id) REFERENCES driver(id),
    INDEX idx_bank_account_driver (driver_id),
    UNIQUE KEY uk_bank_account_driver_active (driver_id, is_active)
) COMMENT 'Driver/owner bank account details for EFT direct deposit';

-- EFT file generation tracking
CREATE TABLE eft_file_generation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT 'FK to payment_batch',
    file_creation_number INT NOT NULL COMMENT 'CPA file creation number used',
    file_name VARCHAR(255) NOT NULL,
    record_count INT NOT NULL DEFAULT 0 COMMENT 'Number of credit/debit records',
    total_credit_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    total_debit_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED, SUBMITTED, PROCESSED, FAILED',
    generated_by VARCHAR(100),
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME,
    notes TEXT,
    CONSTRAINT fk_eft_file_batch FOREIGN KEY (batch_id) REFERENCES payment_batch(id),
    INDEX idx_eft_file_batch (batch_id),
    INDEX idx_eft_file_status (status)
) COMMENT 'Tracks generated EFT files for audit trail';
