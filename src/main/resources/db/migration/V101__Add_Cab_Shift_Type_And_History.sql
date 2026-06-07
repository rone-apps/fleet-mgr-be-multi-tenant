-- V101__Add_Cab_Shift_Type_And_History.sql
-- Add shift type configuration to cab table and create history tracking

-- Add shift_type column to cab table
ALTER TABLE cab
  ADD COLUMN shift_type VARCHAR(20) NOT NULL DEFAULT 'DOUBLE';

-- Add index for shift_type
CREATE INDEX idx_cab_shift_type ON cab(shift_type);

-- Create cab_shift_type_history table to track changes
CREATE TABLE cab_shift_type_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cab_id BIGINT NOT NULL,
  old_shift_type VARCHAR(20),
  new_shift_type VARCHAR(20) NOT NULL,
  changed_at DATETIME NOT NULL,
  changed_by VARCHAR(255),
  reason VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_cab_shift_type_history_cab
    FOREIGN KEY (cab_id) REFERENCES cab(id) ON DELETE CASCADE,

  INDEX idx_cab_shift_type_history_cab (cab_id),
  INDEX idx_cab_shift_type_history_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rollback:
-- DROP TABLE IF EXISTS cab_shift_type_history;
-- ALTER TABLE cab DROP INDEX idx_cab_shift_type;
-- ALTER TABLE cab DROP COLUMN shift_type;
