-- Fix Airport Trip Unique Constraint
-- Change from (cab_number, shift, trip_date) to (cab_number, trip_date)
-- Airport trips are now stored as one record per cab per date with all 24 hours

-- Drop old constraints and indexes
ALTER TABLE airport_trips DROP INDEX IF EXISTS uk_airport_cab_shift_date;
ALTER TABLE airport_trips DROP INDEX IF EXISTS idx_airport_shift;

-- Add new unique constraint on (cab_number, trip_date) only
ALTER TABLE airport_trips
  ADD CONSTRAINT uk_airport_cab_date UNIQUE (cab_number, trip_date);

-- Update all records to shift = 'BOTH' (indicates full 24-hour data)
UPDATE airport_trips SET shift = 'BOTH' WHERE shift IS NOT NULL;
