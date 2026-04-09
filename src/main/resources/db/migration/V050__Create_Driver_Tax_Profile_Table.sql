-- V050: Create driver_tax_profile table for personal tax calculation
-- Stores province, language, marital status, and other required info per driver per tax year

CREATE TABLE driver_tax_profile (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  driver_id BIGINT NOT NULL,
  tax_year INT NOT NULL,
  province VARCHAR(5) NOT NULL,
  language VARCHAR(2) NOT NULL DEFAULT 'EN',
  marital_status VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
  num_dependents INT NOT NULL DEFAULT 0,
  birth_year INT,
  has_disability BOOLEAN NOT NULL DEFAULT FALSE,
  spouse_disability BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME,
  UNIQUE KEY uq_driver_tax_profile (driver_id, tax_year),
  INDEX idx_driver_id (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
