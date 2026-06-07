-- V107: Fix ownership end dates to match cab deactivation dates
--
-- Problem: When cabs were deactivated, ownership records may have incorrect end dates
-- or missing end dates. Ownership should close on the deactivation date.
--
-- This migration:
-- 1. Finds all shifts for INACTIVE cabs
-- 2. Ensures their current ownership records have end_date = cab.deactivated_date
-- 3. Closes any open ownership records (end_date = NULL) for inactive shifts

-- Close open ownership records for inactive shifts
-- Set end_date to cab's deactivation date
UPDATE shift_ownership so
INNER JOIN cab_shift cs ON so.shift_id = cs.id
INNER JOIN cab c ON cs.cab_id = c.id
SET so.end_date = c.deactivated_date
WHERE c.status = 'INACTIVE'
  AND cs.status = 'INACTIVE'
  AND so.end_date IS NULL
  AND c.deactivated_date IS NOT NULL;

-- Log the fix
SELECT CONCAT(
    'Fixed ',
    ROW_COUNT(),
    ' open ownership records for inactive cabs'
) as migration_result;

-- Also fix ownership records that have wrong end_date
-- (end_date should match deactivation_date for inactive cabs)
UPDATE shift_ownership so
INNER JOIN cab_shift cs ON so.shift_id = cs.id
INNER JOIN cab c ON cs.cab_id = c.id
SET so.end_date = c.deactivated_date
WHERE c.status = 'INACTIVE'
  AND cs.status = 'INACTIVE'
  AND c.deactivated_date IS NOT NULL
  AND so.end_date IS NOT NULL
  AND so.end_date != c.deactivated_date
  AND so.end_date > c.deactivated_date; -- Only fix if end_date is after deactivation

-- Log the fix
SELECT CONCAT(
    'Corrected ',
    ROW_COUNT(),
    ' ownership end dates to match deactivation dates'
) as migration_result;

-- Verification: Check for any remaining issues
SELECT
    c.cab_number,
    c.deactivated_date as cab_deactivated,
    cs.shift_type,
    cs.status as shift_status,
    so.start_date,
    so.end_date as ownership_end_date,
    CASE
        WHEN so.end_date IS NULL THEN 'OPEN ownership for inactive shift'
        WHEN so.end_date != c.deactivated_date THEN 'End date mismatch'
        ELSE 'OK'
    END as issue
FROM shift_ownership so
INNER JOIN cab_shift cs ON so.shift_id = cs.id
INNER JOIN cab c ON cs.cab_id = c.id
WHERE c.status = 'INACTIVE'
  AND (so.end_date IS NULL OR so.end_date != c.deactivated_date)
ORDER BY c.cab_number, cs.shift_type, so.start_date;

-- Should return 0 rows with issues after this migration
