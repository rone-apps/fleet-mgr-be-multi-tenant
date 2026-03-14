-- =====================================================
-- V038: Tax Types, Tax Rates, Commission Types, Commission Rates
-- and Category Assignment tables with history tracking
-- =====================================================

-- Tax Types (e.g., HST, GST, PST)
CREATE TABLE tax_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tax_type_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tax Rates (versioned with effective dates)
CREATE TABLE tax_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tax_type_id BIGINT NOT NULL,
    rate DECIMAL(8,4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_rate_type FOREIGN KEY (tax_type_id) REFERENCES tax_type(id),
    INDEX idx_tax_rate_type (tax_type_id),
    INDEX idx_tax_rate_active (is_active),
    INDEX idx_tax_rate_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tax to Expense Category assignments (with history)
CREATE TABLE tax_category_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tax_type_id BIGINT NOT NULL,
    expense_category_id BIGINT NOT NULL,
    assigned_at DATE NOT NULL,
    unassigned_at DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tca_tax_type FOREIGN KEY (tax_type_id) REFERENCES tax_type(id),
    CONSTRAINT fk_tca_expense_cat FOREIGN KEY (expense_category_id) REFERENCES expense_category(id),
    INDEX idx_tca_tax_type (tax_type_id),
    INDEX idx_tca_expense_cat (expense_category_id),
    INDEX idx_tca_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Commission Types (e.g., Credit Card Commission, Account Commission)
CREATE TABLE commission_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_commission_type_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Commission Rates (versioned with effective dates)
CREATE TABLE commission_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commission_type_id BIGINT NOT NULL,
    rate DECIMAL(8,4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_comm_rate_type FOREIGN KEY (commission_type_id) REFERENCES commission_type(id),
    INDEX idx_comm_rate_type (commission_type_id),
    INDEX idx_comm_rate_active (is_active),
    INDEX idx_comm_rate_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Commission to Revenue Category assignments (with history)
CREATE TABLE commission_category_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commission_type_id BIGINT NOT NULL,
    revenue_category_id BIGINT NOT NULL,
    assigned_at DATE NOT NULL,
    unassigned_at DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cca_commission_type FOREIGN KEY (commission_type_id) REFERENCES commission_type(id),
    CONSTRAINT fk_cca_revenue_cat FOREIGN KEY (revenue_category_id) REFERENCES revenue_category(id),
    INDEX idx_cca_commission_type (commission_type_id),
    INDEX idx_cca_revenue_cat (revenue_category_id),
    INDEX idx_cca_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
