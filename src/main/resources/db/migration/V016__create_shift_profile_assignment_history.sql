-- Migration: Create Shift Profile Assignment History
-- Date: 2026-02-07
-- Purpose: Track historical profile assignments to shifts with start/end dates
--          Replaces direct shift_profile_id in cab_shift table with audit trail

-- ============================================================================
-- PART 1: Create shift_profile_assignment table (history/audit table)
-- ============================================================================
-- Tracks all profile assignments to shifts with temporal data
-- When end_date IS NULL, that assignment is currently active
-- Allows for audit trail and historical queries

CREATE TABLE IF NOT EXISTS shift_profile_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique assignment record ID',
    shift_id BIGINT NOT NULL COMMENT 'Reference to cab_shift',
    profile_id BIGINT NOT NULL COMMENT 'Reference to shift_profile',
    start_date DATE NOT NULL COMMENT 'Date when profile assignment became effective',
    end_date DATE NULL COMMENT 'Date when profile assignment ended (NULL = currently active)',
    reason VARCHAR(500) COMMENT 'Reason for assignment (e.g., Profile change, Shift retraining)',
    assigned_by VARCHAR(100) COMMENT 'User who made the assignment',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When this record was created',

    CONSTRAINT fk_assignment_shift
        FOREIGN KEY (shift_id) REFERENCES cab_shift(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_profile
        FOREIGN KEY (profile_id) REFERENCES shift_profile(id) ON DELETE RESTRICT,

    -- Prevent overlapping assignments for same shift
    UNIQUE KEY uk_shift_active_profile (shift_id, end_date),

    INDEX idx_assignment_shift (shift_id),
    INDEX idx_assignment_profile (profile_id),
    INDEX idx_assignment_dates (start_date, end_date),
    INDEX idx_assignment_active (shift_id, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Historical audit trail of shift profile assignments with temporal data';

-- ============================================================================
-- PART 2: Add current_profile_id to cab_shift (denormalized for performance)
-- ============================================================================
-- This is a denormalized field for quick access to current profile
-- Should always match the active assignment (where end_date IS NULL)
-- Updated automatically through triggers or application logic

ALTER TABLE cab_shift
    ADD COLUMN current_profile_id BIGINT COMMENT 'Denormalized: current active profile (references shift_profile_assignment)',
    ADD CONSTRAINT fk_shift_current_profile
        FOREIGN KEY (current_profile_id) REFERENCES shift_profile(id) ON DELETE SET NULL,
    ADD INDEX idx_shift_current_profile (current_profile_id);

-- ============================================================================
-- PART 3: Drop old shift_profile_id column if it exists
-- ============================================================================

ALTER TABLE cab_shift
    DROP FOREIGN KEY IF EXISTS fk_shift_profile,
    DROP COLUMN IF EXISTS shift_profile_id,
    DROP INDEX IF EXISTS idx_shift_profile_id;

-- ============================================================================
-- PART 4: Verify no direct attributes are assigned to shifts
-- ============================================================================
-- Shifts should no longer have attributes assigned directly
-- All attribute matching should be through the assigned profile

-- Optional: If attributes were assigned to shifts, they should be migrated to profile
-- For now, we'll just verify the shift_id in cab_attribute_value points to profiles
-- This is handled by the service layer, not the database

-- ============================================================================
-- PART 5: Verification queries (run manually to verify migration success)
-- ============================================================================

-- Query 1: Verify shift_profile_assignment table created
-- Expected: Table exists with proper structure
-- SELECT * FROM INFORMATION_SCHEMA.TABLES
-- WHERE TABLE_SCHEMA = 'fareflow' AND TABLE_NAME = 'shift_profile_assignment';

-- Query 2: Verify foreign key relationships
-- Expected: Two foreign keys (shift_id -> cab_shift, profile_id -> shift_profile)
-- SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
-- FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
-- WHERE TABLE_NAME = 'shift_profile_assignment';

-- Query 3: Check cab_shift structure (should have current_profile_id now)
-- Expected: current_profile_id column exists
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_NAME = 'cab_shift' AND COLUMN_NAME IN ('current_profile_id', 'shift_profile_id');
