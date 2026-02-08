-- Migration: Move cab attributes to shift level with historical status tracking
-- Date: 2026-02-06
-- Purpose: Refactor architecture to allow each shift (DAY/NIGHT) to have independent attributes
--          and track historical active/inactive status for accurate expense reporting

-- ============================================================================
-- PART 1: Create shift_status_history table
-- ============================================================================
-- Tracks when shifts became active/inactive with full historical audit trail
-- Purpose: Enable historical queries like "was shift active on date X?"
-- Key design: effective_to = NULL means current/ongoing status

CREATE TABLE IF NOT EXISTS shift_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique status record ID',
    shift_id BIGINT NOT NULL COMMENT 'Reference to cab_shift',
    is_active BOOLEAN NOT NULL COMMENT 'Whether shift is active (true) or inactive (false)',
    effective_from DATE NOT NULL COMMENT 'Date when this status became effective',
    effective_to DATE NULL COMMENT 'Date when this status ended (NULL = current status)',
    reason VARCHAR(500) COMMENT 'Reason for status change',
    changed_by VARCHAR(100) COMMENT 'User who made the change',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When this record was created',

    CONSTRAINT fk_shift_status_history_shift
        FOREIGN KEY (shift_id) REFERENCES cab_shift(id) ON DELETE CASCADE,

    INDEX idx_shift_status_shift_id (shift_id),
    INDEX idx_shift_status_dates (effective_from, effective_to),
    INDEX idx_shift_status_active (shift_id, is_active, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Historical tracking of shift active/inactive status with audit trail';

-- ============================================================================
-- PART 2: Add attribute columns to cab_shift table
-- ============================================================================
-- Move attributes from cab level to shift level
-- This allows DAY and NIGHT shifts of the same cab to have different attributes

ALTER TABLE cab_shift
    ADD COLUMN cab_type VARCHAR(20) COMMENT 'Cab type: SEDAN, HANDICAP_VAN (moved from cab)',
    ADD COLUMN share_type VARCHAR(20) COMMENT 'Share type: VOTING_SHARE, NON_VOTING_SHARE (moved from cab)',
    ADD COLUMN has_airport_license BOOLEAN DEFAULT FALSE COMMENT 'Has airport license (moved from cab)',
    ADD COLUMN airport_license_number VARCHAR(50) COMMENT 'Airport license number (moved from cab)',
    ADD COLUMN airport_license_expiry DATE COMMENT 'Airport license expiry date (moved from cab)',
    ADD INDEX idx_shift_cab_type (cab_type),
    ADD INDEX idx_shift_share_type (share_type),
    ADD INDEX idx_shift_airport_license (has_airport_license);

-- ============================================================================
-- PART 3: Migrate attributes from cab to existing cab_shifts
-- ============================================================================
-- Copy attributes from cab table to all existing shifts
-- This ensures all shifts have the attributes from their parent cab
-- NOTE: Using explicit WHERE clause for MySQL safe mode compatibility

UPDATE cab_shift cs
INNER JOIN cab c ON cs.cab_id = c.id
SET
    cs.cab_type = c.cab_type,
    cs.share_type = c.share_type,
    cs.has_airport_license = c.has_airport_license,
    cs.airport_license_number = c.airport_license_number,
    cs.airport_license_expiry = c.airport_license_expiry
WHERE cs.id IS NOT NULL;

-- ============================================================================
-- PART 4: Create missing DAY/NIGHT shifts for cabs that don't have them
-- ============================================================================
-- Ensure every cab has exactly 2 shifts (DAY and NIGHT)
-- If a cab is missing a shift, create it with inherited attributes from cab

INSERT INTO cab_shift (
    cab_id, shift_type, start_time, end_time, current_owner_id, status,
    cab_type, share_type, has_airport_license, airport_license_number, airport_license_expiry,
    created_at, updated_at
)
SELECT
    c.id as cab_id,
    'DAY' as shift_type,
    '06:00' as start_time,
    '18:00' as end_time,
    c.owner_driver_id as current_owner_id,  -- Use cab's owner as shift owner
    'ACTIVE' as status,
    c.cab_type as cab_type,
    c.share_type as share_type,
    c.has_airport_license as has_airport_license,
    c.airport_license_number as airport_license_number,
    c.airport_license_expiry as airport_license_expiry,
    NOW() as created_at,
    NOW() as updated_at
FROM cab c
WHERE NOT EXISTS (
    SELECT 1 FROM cab_shift cs
    WHERE cs.cab_id = c.id AND cs.shift_type = 'DAY'
)
AND c.owner_driver_id IS NOT NULL;  -- Only for cabs that have an owner (will need manual setup for company-owned)

INSERT INTO cab_shift (
    cab_id, shift_type, start_time, end_time, current_owner_id, status,
    cab_type, share_type, has_airport_license, airport_license_number, airport_license_expiry,
    created_at, updated_at
)
SELECT
    c.id as cab_id,
    'NIGHT' as shift_type,
    '18:00' as start_time,
    '06:00' as end_time,
    c.owner_driver_id as current_owner_id,  -- Use cab's owner as shift owner
    'ACTIVE' as status,
    c.cab_type as cab_type,
    c.share_type as share_type,
    c.has_airport_license as has_airport_license,
    c.airport_license_number as airport_license_number,
    c.airport_license_expiry as airport_license_expiry,
    NOW() as created_at,
    NOW() as updated_at
FROM cab c
WHERE NOT EXISTS (
    SELECT 1 FROM cab_shift cs
    WHERE cs.cab_id = c.id AND cs.shift_type = 'NIGHT'
)
AND c.owner_driver_id IS NOT NULL;  -- Only for cabs that have an owner (will need manual setup for company-owned)

-- ============================================================================
-- PART 5: Create initial shift_status_history for all shifts
-- ============================================================================
-- Every shift needs at least one status history record
-- Use the shift's current status and the cab's creation date as effective date
-- This creates the audit trail starting point

INSERT INTO shift_status_history (
    shift_id, is_active, effective_from, effective_to, reason, changed_by, created_at
)
SELECT
    cs.id as shift_id,
    (cs.status = 'ACTIVE') as is_active,  -- Convert ACTIVE/INACTIVE to boolean
    DATE(COALESCE(c.created_at, '2020-01-01')) as effective_from,  -- Use cab creation date
    NULL as effective_to,  -- NULL means current status
    CONCAT('Initial status from cab creation (cab status: ', c.status, ')') as reason,
    'SYSTEM_MIGRATION' as changed_by,
    NOW() as created_at
FROM cab_shift cs
INNER JOIN cab c ON cs.cab_id = c.id
WHERE NOT EXISTS (
    SELECT 1 FROM shift_status_history ssh
    WHERE ssh.shift_id = cs.id
);

-- ============================================================================
-- PART 6: Update cab_attribute_value to support shift_id
-- ============================================================================
-- Add shift relationship to cab_attribute_value
-- This allows attributes to be assigned at shift level instead of just cab level

ALTER TABLE cab_attribute_value
    ADD COLUMN shift_id BIGINT COMMENT 'Reference to cab_shift (new relationship)',
    ADD CONSTRAINT fk_cab_attr_shift
        FOREIGN KEY (shift_id) REFERENCES cab_shift(id) ON DELETE CASCADE,
    ADD INDEX idx_cab_attr_shift (shift_id);

-- For existing attributes, assign them to the DAY shift by default
-- This maintains backward compatibility - can be manually reassigned later
UPDATE cab_attribute_value cav
INNER JOIN cab_shift cs ON cav.cab_id = cs.cab_id AND cs.shift_type = 'DAY'
SET cav.shift_id = cs.id
WHERE cav.shift_id IS NULL AND cs.shift_type = 'DAY';

-- ============================================================================
-- PART 7: Verification queries (run manually to verify migration success)
-- ============================================================================

-- Query 1: Check all cabs have exactly 2 shifts
-- Expected: All cabs should appear with shift_count = 2
-- SELECT c.id, c.cab_number, COUNT(cs.id) as shift_count
-- FROM cab c
-- LEFT JOIN cab_shift cs ON c.id = cs.cab_id
-- GROUP BY c.id, c.cab_number
-- HAVING shift_count != 2;  -- Should return no rows

-- Query 2: Check all shifts have attributes copied
-- Expected: No rows returned (all shifts should have cab_type set)
-- SELECT cs.id, c.cab_number, cs.shift_type, cs.cab_type
-- FROM cab_shift cs
-- INNER JOIN cab c ON cs.cab_id = c.id
-- WHERE cs.cab_type IS NULL;

-- Query 3: Check all shifts have status history
-- Expected: All shifts should have at least one status history record
-- SELECT cs.id, c.cab_number, cs.shift_type,
--        COUNT(ssh.id) as status_records
-- FROM cab_shift cs
-- INNER JOIN cab c ON cs.cab_id = c.id
-- LEFT JOIN shift_status_history ssh ON cs.id = ssh.shift_id
-- GROUP BY cs.id, c.cab_number, cs.shift_type
-- HAVING status_records = 0;  -- Should return no rows

-- Query 4: Check current status records exist
-- Expected: Every shift should have exactly one record with effective_to = NULL
-- SELECT cs.id, c.cab_number, COUNT(ssh.id) as current_status_count
-- FROM cab_shift cs
-- LEFT JOIN shift_status_history ssh ON cs.id = ssh.shift_id AND ssh.effective_to IS NULL
-- LEFT JOIN cab c ON cs.cab_id = c.id
-- GROUP BY cs.id, c.cab_number
-- HAVING current_status_count != 1;  -- Should return no rows

-- Query 5: Check shift_id assignments in cab_attribute_value
-- Expected: All attributes should have shift_id assigned to the DAY shift
-- SELECT cav.id, c.cab_number, cs.shift_type, cav.shift_id
-- FROM cab_attribute_value cav
-- INNER JOIN cab c ON cav.cab_id = c.id
-- LEFT JOIN cab_shift cs ON cav.shift_id = cs.id
-- WHERE cav.shift_id IS NULL;  -- Should return no rows

-- ============================================================================
-- PART 8: Column removal (commented out - run ONLY after verification!)
-- ============================================================================
-- NOTE: These column removals should ONLY be done after:
-- 1. All verification queries pass
-- 2. Testing in staging environment succeeds
-- 3. Backup of database is taken
-- 4. Frontend and backend code updated to use shift-level attributes

-- ALTER TABLE cab
--     DROP COLUMN cab_type,
--     DROP COLUMN share_type,
--     DROP COLUMN cab_shift_type,
--     DROP COLUMN has_airport_license,
--     DROP COLUMN airport_license_number,
--     DROP COLUMN airport_license_expiry,
--     DROP COLUMN status;
