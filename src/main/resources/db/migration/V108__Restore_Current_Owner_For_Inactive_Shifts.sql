-- V108: Restore current_owner_id for inactive shifts
--
-- Problem: When cabs were deactivated, cab_shift.current_owner_id was cleared/lost
-- This prevents reactivation with "same owners" mode because there's no current owner to use
--
-- Solution: Look up the most recent ownership record and restore current_owner_id

-- Update current_owner_id for inactive shifts based on their most recent ownership
UPDATE cab_shift cs
INNER JOIN (
    SELECT
        so.shift_id,
        so.owner_id,
        ROW_NUMBER() OVER (PARTITION BY so.shift_id ORDER BY so.start_date DESC) as rn
    FROM shift_ownership so
) latest_ownership ON cs.id = latest_ownership.shift_id AND latest_ownership.rn = 1
SET cs.current_owner_id = latest_ownership.owner_id
WHERE cs.status = 'INACTIVE'
  AND (cs.current_owner_id IS NULL OR cs.current_owner_id != latest_ownership.owner_id);

-- Log the fix
SELECT CONCAT(
    'Restored current_owner_id for ',
    ROW_COUNT(),
    ' inactive shifts'
) as migration_result;

-- Verification: Check for inactive shifts with no owner
SELECT
    c.cab_number,
    cs.shift_type,
    cs.status,
    cs.current_owner_id,
    'WARNING: Inactive shift with no current owner' as issue
FROM cab_shift cs
INNER JOIN cab c ON cs.cab_id = c.id
WHERE cs.status = 'INACTIVE'
  AND cs.current_owner_id IS NULL;

-- Should return 0 rows after this migration
