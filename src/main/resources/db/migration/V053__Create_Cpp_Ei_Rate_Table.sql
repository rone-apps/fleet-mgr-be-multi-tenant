-- V053: Create cpp_ei_rate table for configurable CPP and EI contribution rates
-- Stores annual CPP/EI rates, max pensionable/insurable, and basic exemptions

CREATE TABLE cpp_ei_rate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tax_year INT NOT NULL UNIQUE,
  cpp_employee_rate DECIMAL(6,4) NOT NULL,
  cpp_max_pensionable DECIMAL(12,2) NOT NULL,
  cpp_basic_exemption DECIMAL(12,2) NOT NULL,
  ei_employee_rate DECIMAL(6,4) NOT NULL,
  ei_max_insurable DECIMAL(12,2) NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
