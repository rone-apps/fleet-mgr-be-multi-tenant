-- Create legacy_driver table to map legacy driver IDs to driver_number
-- This allows mapping legacy charges to current drivers via driver_number (stable business key)
-- Name is not stored here - use current Driver table for driver names

CREATE TABLE legacy_driver (
  id BIGINT NOT NULL,
  driver_number VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_legacy_driver_number (driver_number),
  INDEX idx_legacy_driver_number (driver_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Legacy driver ID to driver_number mapping - minimal table for charge attribution only';

-- Rollback:
-- DROP TABLE legacy_driver;
