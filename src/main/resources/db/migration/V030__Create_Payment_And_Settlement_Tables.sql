-- Payment Method Table
CREATE TABLE IF NOT EXISTS payment_method (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    method_name VARCHAR(100) NOT NULL,
    method_code VARCHAR(20) NOT NULL UNIQUE,
    requires_reference BOOLEAN NOT NULL DEFAULT true,
    requires_bank_details BOOLEAN NOT NULL DEFAULT false,
    is_automatic BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,

    KEY idx_method_code (method_code),
    KEY idx_active (is_active)
);

-- Payment Table
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    statement_id BIGINT NOT NULL,
    payment_batch_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method_id BIGINT NOT NULL,
    reference_number VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    posted_at TIMESTAMP,
    posted_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,

    FOREIGN KEY (statement_id) REFERENCES invoice(id),
    FOREIGN KEY (payment_batch_id) REFERENCES payment_batch(id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_method(id),

    UNIQUE KEY unique_payment_per_statement (statement_id, payment_batch_id),
    KEY idx_payment_date (payment_date),
    KEY idx_status (status),
    KEY idx_statement (statement_id)
);

-- Payment Batch Table
CREATE TABLE IF NOT EXISTS payment_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_number VARCHAR(50) NOT NULL UNIQUE,
    batch_date DATE NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    payment_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    posted_at TIMESTAMP,
    posted_by BIGINT,
    processed_at TIMESTAMP,
    processed_by BIGINT,
    actual_payments_made DECIMAL(10,2),
    reconciliation_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,

    KEY idx_batch_date (batch_date),
    KEY idx_status (status),
    KEY idx_period (period_start, period_end)
);

-- Statement Audit Log Table
CREATE TABLE IF NOT EXISTS statement_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    statement_id BIGINT NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    change_description TEXT,
    changed_fields JSON,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,

    FOREIGN KEY (statement_id) REFERENCES invoice(id),
    KEY idx_statement (statement_id),
    KEY idx_change_type (change_type),
    KEY idx_changed_at (changed_at)
);

-- Enhance Invoice table for statement lifecycle
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' AFTER balance_due;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS statement_version INT NOT NULL DEFAULT 1 AFTER status;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS parent_statement_id BIGINT AFTER statement_version;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS pdf_path VARCHAR(500) AFTER parent_statement_id;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS pdf_generated_at TIMESTAMP AFTER pdf_path;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS line_items_json JSON AFTER pdf_generated_at;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS posted_at TIMESTAMP AFTER line_items_json;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS posted_by BIGINT AFTER posted_at;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP AFTER posted_by;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS locked_by BIGINT AFTER locked_at;

-- Create indexes on invoice for payment workflow
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoice(status);
CREATE INDEX IF NOT EXISTS idx_invoice_period ON invoice(billing_period_start, billing_period_end);

-- Insert default payment methods
INSERT IGNORE INTO payment_method (method_name, method_code, requires_reference, display_order, is_active, created_by, created_at)
VALUES
    ('Cheque', 'CHQ', true, 1, true, 1, CURRENT_TIMESTAMP),
    ('Direct Deposit', 'DD', true, 2, true, 1, CURRENT_TIMESTAMP),
    ('Cash', 'CASH', false, 3, true, 1, CURRENT_TIMESTAMP),
    ('E-Transfer', 'ETRF', true, 4, true, 1, CURRENT_TIMESTAMP);
