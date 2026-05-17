-- Create legacy customer charge table (port of existing customercharges table)
-- This table supports tenants transitioning from legacy system to modern account_charge system

CREATE TABLE legacy_customer_charge (
  id BIGINT NOT NULL AUTO_INCREMENT,
  amount DOUBLE NOT NULL,
  date DATE DEFAULT NULL,
  payment DOUBLE DEFAULT NULL,
  cab_id BIGINT DEFAULT NULL,
  customer_id BIGINT DEFAULT NULL,
  driver_id BIGINT DEFAULT NULL,
  notes VARCHAR(255) DEFAULT NULL,
  type VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_legacy_charge_cab FOREIGN KEY (cab_id) REFERENCES cab (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_legacy_charge_driver FOREIGN KEY (driver_id) REFERENCES driver (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_legacy_charge_customer FOREIGN KEY (customer_id) REFERENCES account_customer (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  INDEX idx_legacy_charge_date (date),
  INDEX idx_legacy_charge_driver (driver_id),
  INDEX idx_legacy_charge_customer (customer_id),
  INDEX idx_legacy_charge_cab (cab_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Legacy customer charges table for tenants transitioning to modern account_charge system';

-- Rollback:
-- DROP TABLE legacy_customer_charge;
