-- V102__Add_Fleet_Added_Date_To_Cab.sql
-- Add fleet_added_date column to track when cab was physically added to fleet

ALTER TABLE cab
  ADD COLUMN fleet_added_date DATE NULL;

-- Set default to created_at date for existing cabs
UPDATE cab
SET fleet_added_date = DATE(created_at)
WHERE fleet_added_date IS NULL;

-- Add index for fleet_added_date
CREATE INDEX idx_cab_fleet_added_date ON cab(fleet_added_date);

-- Rollback:
-- ALTER TABLE cab DROP INDEX idx_cab_fleet_added_date;
-- ALTER TABLE cab DROP COLUMN fleet_added_date;
