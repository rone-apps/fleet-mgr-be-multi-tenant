-- V106: Fix shift status for deactivated cabs
--
-- Problem: Shifts for deactivated cabs were not being marked as INACTIVE
-- because the old deactivation logic only created status history records
-- but did not update the cab_shift.status field.
--
-- This migration:
-- 1. Finds all shifts for INACTIVE cabs that are still marked as ACTIVE
-- 2. Updates their status to INACTIVE
-- 3. Verifies shifts match their cab's status

-- Update shifts to INACTIVE where their cab is INACTIVE
UPDATE cab_shift cs
INNER JOIN cab c ON cs.cab_id = c.id
SET cs.status = 'INACTIVE'
WHERE c.status = 'INACTIVE'
  AND cs.status = 'ACTIVE';

-- Log the fix
SELECT CONCAT(
    'Fixed ',
    ROW_COUNT(),
    ' shifts that were ACTIVE but belonged to INACTIVE cabs'
) as migration_result;

-- Verification: Check for any remaining mismatches
SELECT
    c.cab_number,
    c.status as cab_status,
    cs.shift_type,
    cs.status as shift_status,
    'MISMATCH: Shift should be INACTIVE' as issue
FROM cab_shift cs
INNER JOIN cab c ON cs.cab_id = c.id
WHERE c.status = 'INACTIVE'
  AND cs.status = 'ACTIVE';

-- Should return 0 rows after this migration
