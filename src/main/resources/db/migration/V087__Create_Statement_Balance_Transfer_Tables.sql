-- V087__Create_Statement_Balance_Transfer_Tables.sql
-- Statement Balance Transfer Feature - Phase 1: Database Schema
-- Allows drivers/owners to transfer statement balances between each other

-- Main transfer configuration and tracking table
CREATE TABLE statement_balance_transfer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_number VARCHAR(50) UNIQUE NOT NULL,

    -- Source (from)
    source_person_id BIGINT NOT NULL,
    source_person_type VARCHAR(20) NOT NULL,
    source_person_name VARCHAR(255),

    -- Target (to)
    target_person_id BIGINT NOT NULL,
    target_person_type VARCHAR(20) NOT NULL,
    target_person_name VARCHAR(255),

    -- Configuration
    transfer_type VARCHAR(20) NOT NULL COMMENT 'ONE_TIME, RECURRING',
    balance_direction VARCHAR(20) NOT NULL COMMENT 'POSITIVE_ONLY, BOTH',

    -- Amounts
    transfer_amount DECIMAL(10, 2) NOT NULL,
    transferred_amount DECIMAL(10, 2) DEFAULT 0.00,
    remaining_amount DECIMAL(10, 2) NOT NULL,

    -- Date range
    start_date DATE NOT NULL,
    end_date DATE,
    statement_period_from DATE,
    statement_period_to DATE,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, COMPLETED, CANCELLED, SUSPENDED',

    -- Metadata
    description TEXT,
    notes TEXT,
    reason VARCHAR(500),

    -- Audit
    created_at DATETIME NOT NULL,
    created_by BIGINT,
    updated_at DATETIME,
    updated_by BIGINT,
    cancelled_at DATETIME,
    cancelled_by BIGINT,
    cancellation_reason TEXT,

    CONSTRAINT fk_transfer_source FOREIGN KEY (source_person_id) REFERENCES driver(id),
    CONSTRAINT fk_transfer_target FOREIGN KEY (target_person_id) REFERENCES driver(id),

    INDEX idx_transfer_source (source_person_id, status),
    INDEX idx_transfer_target (target_person_id, status),
    INDEX idx_transfer_dates (start_date, end_date),
    INDEX idx_transfer_status (status),
    INDEX idx_transfer_period (statement_period_from, statement_period_to),
    INDEX idx_transfer_number (transfer_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Statement balance transfer configuration and tracking';

-- Transfer application history (audit trail)
CREATE TABLE statement_balance_transfer_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    transfer_id BIGINT NOT NULL,
    source_statement_id BIGINT,
    target_statement_id BIGINT,

    transfer_amount DECIMAL(10, 2) NOT NULL,
    applied_period_from DATE NOT NULL,
    applied_period_to DATE NOT NULL,

    applied_at DATETIME NOT NULL,
    applied_by BIGINT,
    description TEXT,

    -- Reversal tracking
    is_reversed TINYINT(1) DEFAULT 0,
    reversed_at DATETIME,
    reversed_by BIGINT,
    reversal_reason TEXT,

    CONSTRAINT fk_transfer_history_transfer FOREIGN KEY (transfer_id)
        REFERENCES statement_balance_transfer(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_history_source_stmt FOREIGN KEY (source_statement_id)
        REFERENCES statements(id) ON DELETE SET NULL,
    CONSTRAINT fk_transfer_history_target_stmt FOREIGN KEY (target_statement_id)
        REFERENCES statements(id) ON DELETE SET NULL,

    INDEX idx_history_transfer (transfer_id),
    INDEX idx_history_source_stmt (source_statement_id),
    INDEX idx_history_target_stmt (target_statement_id),
    INDEX idx_history_period (applied_period_from, applied_period_to),
    INDEX idx_history_reversed (is_reversed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Statement balance transfer application audit trail';

-- Add expense category for outgoing transfers
INSERT INTO expense_category (category_code, category_name, description, is_active, created_at, updated_at)
VALUES ('BALANCE_TRANSFER_OUT', 'Balance Transfer (Outgoing)', 'Transfer of statement balance to another person', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Rollback instructions:
-- DROP TABLE statement_balance_transfer_history;
-- DROP TABLE statement_balance_transfer;
-- DELETE FROM expense_category WHERE category_code = 'BALANCE_TRANSFER_OUT';
