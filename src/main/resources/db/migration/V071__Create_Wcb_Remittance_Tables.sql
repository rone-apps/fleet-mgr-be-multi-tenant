-- Create WCB Remittance header and payment line tables
CREATE TABLE wcb_remittance (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_id   BIGINT NOT NULL,
    payee_name   VARCHAR(200) NULL,
    payee_number VARCHAR(100) NULL,
    cheque_date  DATE NULL,
    cheque_number VARCHAR(100) NULL,
    total_amount DECIMAL(12,2) NULL,
    currency     VARCHAR(10) NOT NULL DEFAULT 'CAD',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wcb_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id)
);

CREATE INDEX idx_wcb_remittance_receipt ON wcb_remittance (receipt_id);
CREATE INDEX idx_wcb_remittance_payee ON wcb_remittance (payee_number);

CREATE TABLE wcb_remittance_line (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    remittance_id    BIGINT NOT NULL,
    invoice_no       VARCHAR(100) NULL,
    claim_number     VARCHAR(100) NULL,
    customer_name    VARCHAR(200) NULL,
    service_date     DATE NULL,
    service_code     VARCHAR(100) NULL,
    invoice_amount   DECIMAL(10,2) NULL,
    unit_description VARCHAR(200) NULL,
    rate             DECIMAL(10,4) NULL,
    amount           DECIMAL(10,2) NULL,
    explanation      VARCHAR(500) NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wcb_line_remittance FOREIGN KEY (remittance_id) REFERENCES wcb_remittance(id) ON DELETE CASCADE
);

CREATE INDEX idx_wcb_line_remittance ON wcb_remittance_line (remittance_id);
CREATE INDEX idx_wcb_line_claim ON wcb_remittance_line (claim_number);
