-- Create lease_expense table
CREATE TABLE IF NOT EXISTS `lease_expense` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `driver_id` BIGINT NOT NULL,
  `cab_id` BIGINT NOT NULL,
  `lease_date` DATE NOT NULL,
  `day_of_week` ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
  `shift_type` ENUM('MORNING', 'EVENING') NOT NULL,
  `cab_type` ENUM('SEDAN', 'HANDICAP_VAN') NOT NULL,
  `has_airport_license` BIT(1) NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `rate_id` BIGINT NOT NULL,
  `base_amount` DECIMAL(10,2) NOT NULL,
  `miles_driven` DECIMAL(10,2),
  `mileage_amount` DECIMAL(10,2) DEFAULT 0.00,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `shift_id` BIGINT,
  `notes` VARCHAR(500),
  `is_paid` BIT(1) DEFAULT 0,
  `paid_date` DATE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6),
  PRIMARY KEY (`id`),
  KEY `idx_driver_date` (`driver_id`, `lease_date`),
  KEY `idx_cab_date` (`cab_id`, `lease_date`),
  KEY `idx_lease_date` (`lease_date`),
  KEY `idx_shift` (`shift_id`),
  KEY `idx_paid_status` (`is_paid`),
  CONSTRAINT `fk_lease_expense_plan` FOREIGN KEY (`plan_id`) REFERENCES `lease_plan` (`id`),
  CONSTRAINT `fk_lease_expense_rate` FOREIGN KEY (`rate_id`) REFERENCES `lease_rate` (`id`),
  CONSTRAINT `fk_lease_expense_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
  CONSTRAINT `fk_lease_expense_cab` FOREIGN KEY (`cab_id`) REFERENCES `cab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add index for unpaid driver expenses (common query)
CREATE INDEX `idx_driver_unpaid` ON `lease_expense` (`driver_id`, `is_paid`, `lease_date`);
