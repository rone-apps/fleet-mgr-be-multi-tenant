-- ==============================================================
-- MANUAL SQL MIGRATION FOR SHIFT TABLES
-- Run this directly in MySQL if you want immediate update
-- ==============================================================

USE your_database_name;  -- CHANGE THIS to your actual database name

-- Step 1: Add new ENUM values (keeps MORNING/EVENING temporarily)
ALTER TABLE cab_shift 
MODIFY COLUMN shift_type ENUM('MORNING', 'EVENING', 'DAY', 'NIGHT') NOT NULL;

-- Step 2: Migrate existing data
UPDATE cab_shift SET shift_type = 'DAY' WHERE shift_type = 'MORNING';
UPDATE cab_shift SET shift_type = 'NIGHT' WHERE shift_type = 'EVENING';

-- Step 3: Remove old ENUM values
ALTER TABLE cab_shift 
MODIFY COLUMN shift_type ENUM('DAY', 'NIGHT') NOT NULL;

-- Step 4: Update shift times for DAY shifts (from old MORNING times)
UPDATE cab_shift 
SET start_time = '06:00', end_time = '18:00' 
WHERE shift_type = 'DAY' AND start_time = '06:00' AND end_time = '14:00';

-- Step 5: Update shift times for NIGHT shifts (from old EVENING times)
UPDATE cab_shift 
SET start_time = '18:00', end_time = '06:00' 
WHERE shift_type = 'NIGHT' AND start_time = '14:00' AND end_time = '22:00';

-- Step 6: Verify the changes
SELECT 
    id, 
    cab_id,
    shift_type, 
    start_time, 
    end_time,
    status
FROM cab_shift
ORDER BY cab_id, shift_type;

-- Expected results:
-- shift_type should be 'DAY' or 'NIGHT' only
-- DAY shifts should have start_time='06:00', end_time='18:00' (or custom)
-- NIGHT shifts should have start_time='18:00', end_time='06:00' (or custom)

SELECT '✅ Migration complete! shift_type now uses DAY/NIGHT' AS status;
