-- Migration: Update shift tables to use DAY/NIGHT instead of MORNING/EVENING
-- Date: 2024-11-28
-- This migration safely updates the shift_type ENUM values

-- Step 1: Add new ENUM values to shift_type (keeps old values temporarily)
ALTER TABLE cab_shift 
MODIFY COLUMN shift_type ENUM('MORNING', 'EVENING', 'DAY', 'NIGHT') NOT NULL;

-- Step 2: Update existing data from old values to new values
UPDATE cab_shift SET shift_type = 'DAY' WHERE shift_type = 'MORNING';
UPDATE cab_shift SET shift_type = 'NIGHT' WHERE shift_type = 'EVENING';

-- Step 3: Remove old ENUM values (now that all data is migrated)
ALTER TABLE cab_shift 
MODIFY COLUMN shift_type ENUM('DAY', 'NIGHT') NOT NULL;

-- Step 4: Update shift times for existing DAY shifts
UPDATE cab_shift 
SET start_time = '06:00', end_time = '18:00' 
WHERE shift_type = 'DAY' AND start_time = '06:00' AND end_time = '14:00';

-- Step 5: Update shift times for existing NIGHT shifts  
UPDATE cab_shift 
SET start_time = '18:00', end_time = '06:00' 
WHERE shift_type = 'NIGHT' AND start_time = '14:00' AND end_time = '22:00';

-- Step 6: Ensure status ENUM is correct
ALTER TABLE cab_shift 
MODIFY COLUMN status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE';

