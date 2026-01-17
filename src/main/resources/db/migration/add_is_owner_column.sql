-- Migration to add is_owner column to driver table
-- This ensures existing drivers get a default value of FALSE

-- Add the column with default value
ALTER TABLE driver 
ADD COLUMN IF NOT EXISTS is_owner BOOLEAN NOT NULL DEFAULT FALSE;

-- Update any existing NULL values (shouldn't be any with the above, but just in case)
UPDATE driver 
SET is_owner = FALSE 
WHERE is_owner IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN driver.is_owner IS 'Indicates if the driver owns their vehicle';
