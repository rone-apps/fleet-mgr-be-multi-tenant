-- V051: Create income_tax_bracket table for configurable tax brackets
-- Stores federal and provincial tax brackets by year

CREATE TABLE income_tax_bracket (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tax_year INT NOT NULL,
  jurisdiction VARCHAR(10) NOT NULL,
  bracket_order INT NOT NULL,
  min_income DECIMAL(12,2) NOT NULL,
  max_income DECIMAL(12,2),
  rate DECIMAL(6,4) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_bracket_year_jurisdiction (tax_year, jurisdiction),
  INDEX idx_bracket_order (bracket_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
