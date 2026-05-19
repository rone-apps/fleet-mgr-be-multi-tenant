-- V088__Create_Transfer_Execution_Table.sql
-- Creates the statement_transfer_execution table for tracking calculated transfer executions
-- that flow through approval workflow before being applied to statements

CREATE TABLE statement_transfer_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_number VARCHAR(50) UNIQUE NOT NULL,

    -- Link to configuration
    transfer_config_id BIGINT NOT NULL,
    config_transfer_number VARCHAR(50), -- Denormalized for display

    -- Specific period
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,

    -- Calculated details
    calculated_amount DECIMAL(10, 2) NOT NULL,
    source_balance_snapshot DECIMAL(10, 2), -- Balance when calculated
    source_statement_id_snapshot BIGINT, -- Which statement balance came from
    calculation_date DATETIME NOT NULL,
    calculated_by BIGINT,
    calculation_notes TEXT,

    -- Person details (denormalized for immutability)
    source_person_id BIGINT NOT NULL,
    source_person_name VARCHAR(255),
    target_person_id BIGINT NOT NULL,
    target_person_name VARCHAR(255),

    -- Status workflow
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Flow: PENDING → APPROVED → APPLIED → FINALIZED
    --       PENDING → REJECTED

    -- Approval tracking
    approved_date DATETIME,
    approved_by BIGINT,
    approval_notes TEXT,

    -- Rejection tracking
    rejected_date DATETIME,
    rejected_by BIGINT,
    rejection_reason TEXT,

    -- Application tracking (when statement generated)
    applied_date DATETIME,
    applied_by BIGINT,
    source_statement_id BIGINT,
    target_statement_id BIGINT,

    -- Finalization tracking (when statement finalized)
    finalized_date DATETIME,

    -- Audit
    created_at DATETIME NOT NULL,
    updated_at DATETIME,

    CONSTRAINT fk_execution_config FOREIGN KEY (transfer_config_id)
        REFERENCES statement_balance_transfer(id) ON DELETE RESTRICT,
    CONSTRAINT fk_execution_source_stmt FOREIGN KEY (source_statement_id)
        REFERENCES statements(id) ON DELETE SET NULL,
    CONSTRAINT fk_execution_target_stmt FOREIGN KEY (target_statement_id)
        REFERENCES statements(id) ON DELETE SET NULL,
    CONSTRAINT fk_execution_source_snapshot FOREIGN KEY (source_statement_id_snapshot)
        REFERENCES statements(id) ON DELETE SET NULL,

    INDEX idx_execution_config (transfer_config_id),
    INDEX idx_execution_period (period_from, period_to),
    INDEX idx_execution_status (status),
    INDEX idx_execution_source_person (source_person_id, period_from, period_to),
    INDEX idx_execution_target_person (target_person_id, period_from, period_to),
    INDEX idx_execution_source_stmt (source_statement_id),
    INDEX idx_execution_target_stmt (target_statement_id),
    UNIQUE INDEX idx_execution_config_period (transfer_config_id, period_from, period_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rollback:
-- DROP TABLE statement_transfer_execution;
