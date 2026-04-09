-- V056: Create spare_machine table for credit card machine catalog
-- Tracks spare payment terminal machines and their virtual cab ID assignment (10000-11000 range)

CREATE TABLE spare_machine (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_name VARCHAR(50) NOT NULL UNIQUE,
  virtual_cab_id INT NOT NULL UNIQUE,
  merchant_number VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME,
  INDEX idx_virtual_cab (virtual_cab_id),
  INDEX idx_merchant (merchant_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
