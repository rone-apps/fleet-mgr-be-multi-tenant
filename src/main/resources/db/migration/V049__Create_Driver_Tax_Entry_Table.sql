-- V049: Create driver_tax_entry table for personal tax deductions
-- Supports T slips (T4, T4A, T5, etc.), RRSP contributions, donations, and other deductions

CREATE TABLE driver_tax_entry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  driver_id BIGINT NOT NULL,
  driver_name VARCHAR(255),
  tax_year INT NOT NULL,
  entry_type VARCHAR(30) NOT NULL,
  slip_type VARCHAR(20),
  box_label VARCHAR(100),
  issuer_name VARCHAR(255),
  amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  notes VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME,
  INDEX idx_driver_tax_year (driver_id, tax_year),
  INDEX idx_entry_type (entry_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
