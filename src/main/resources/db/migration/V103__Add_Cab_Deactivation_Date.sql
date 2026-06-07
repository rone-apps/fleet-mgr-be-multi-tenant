-- V103__Add_Cab_Deactivation_Date.sql
-- Add deactivated_date field to track when cab was physically removed from fleet

ALTER TABLE cab
  ADD COLUMN deactivated_date DATE NULL
  AFTER fleet_added_date;

CREATE INDEX idx_cab_deactivated_date ON cab(deactivated_date);

-- Set deactivated_date for currently INACTIVE cabs (default to created_at date as best guess)
UPDATE cab
SET deactivated_date = DATE(created_at)
WHERE status = 'INACTIVE' AND deactivated_date IS NULL;

-- Rollback:
-- ALTER TABLE cab DROP INDEX idx_cab_deactivated_date;
-- ALTER TABLE cab DROP COLUMN deactivated_date;
