-- V105__Backfill_Missing_Shift_Ownerships.sql
-- Backfill missing shift_ownership records for existing cabs
-- This migration finds shifts that have owners but no ownership records
-- and creates initial ownership records with the cab's fleet_added_date

INSERT INTO shift_ownership (
    shift_id,
    owner_id,
    start_date,
    end_date,
    acquisition_type,
    notes,
    created_at
)
SELECT
    cs.id as shift_id,
    cs.current_owner_id as owner_id,
    COALESCE(c.fleet_added_date, DATE(c.created_at)) as start_date,
    NULL as end_date,
    'INITIAL_ASSIGNMENT' as acquisition_type,
    'Backfilled from existing data' as notes,
    NOW() as created_at
FROM cab_shift cs
INNER JOIN cab c ON cs.cab_id = c.id
WHERE cs.current_owner_id IS NOT NULL
  AND cs.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM shift_ownership so
      WHERE so.shift_id = cs.id
        AND so.end_date IS NULL
  );

-- Log results
SELECT CONCAT('Backfilled ', ROW_COUNT(), ' missing shift ownership records') as result;
