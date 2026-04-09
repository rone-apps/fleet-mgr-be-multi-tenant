-- V052: Create tax_credit table for configurable tax credits
-- Stores basic personal amount, age credit, disability credit, donation credit, etc.

CREATE TABLE tax_credit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tax_year INT NOT NULL,
  jurisdiction VARCHAR(10) NOT NULL,
  credit_code VARCHAR(30) NOT NULL,
  credit_name VARCHAR(100) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  rate DECIMAL(6,4),
  description VARCHAR(500),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL,
  INDEX idx_credit_year_jurisdiction (tax_year, jurisdiction),
  INDEX idx_credit_code (credit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
