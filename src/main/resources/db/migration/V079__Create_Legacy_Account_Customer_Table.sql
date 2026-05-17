-- Create legacy account customer table (port of existing accountcustomer table)
-- This table supports tenants transitioning from legacy system to modern account_customer system
-- IDs are different between legacy and modern, so we need separate tables

CREATE TABLE legacy_account_customer (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  street VARCHAR(255) NOT NULL,
  city VARCHAR(255) DEFAULT NULL,
  province VARCHAR(255) DEFAULT NULL,
  postal_code VARCHAR(255) DEFAULT NULL,
  contact VARCHAR(255) DEFAULT NULL,
  phone VARCHAR(255) DEFAULT NULL,
  email VARCHAR(255) DEFAULT NULL,
  credit_limit DOUBLE DEFAULT NULL,
  date DATE DEFAULT NULL,
  notes VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_legacy_customer_id (customer_id),
  INDEX idx_legacy_customer_name (name),
  INDEX idx_legacy_customer_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Legacy account customer table for tenants transitioning to modern account_customer system';

-- Rollback:
-- DROP TABLE legacy_account_customer;
