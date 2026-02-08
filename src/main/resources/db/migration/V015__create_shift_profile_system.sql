-- Migration: Create Shift Profile System
-- Date: 2026-02-07
-- Purpose: Create reusable shift profiles bundling attributes for automated categorization
--          and profile-based expense matching, simplifying complex AttributeRulesBuilder system

-- ============================================================================
-- PART 1: Create shift_profile table
-- ============================================================================
-- Central table for defining reusable shift attribute bundles
-- Static attributes (cab_type, share_type, airport_license, shift_type) can be NULL = "any"
-- System profiles are protected from deletion but can be deactivated

CREATE TABLE IF NOT EXISTS shift_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique profile ID',
    profile_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Unique profile identifier (e.g., STANDARD_SEDAN_VOTING)',
    profile_name VARCHAR(100) NOT NULL COMMENT 'Display name (e.g., Standard Sedan - Voting)',
    description VARCHAR(500) COMMENT 'Detailed description of this profile',

    -- Static attributes (nullable = "any")
    cab_type VARCHAR(20) COMMENT 'Cab type: SEDAN, HANDICAP_VAN (NULL = any)',
    share_type VARCHAR(20) COMMENT 'Share type: VOTING_SHARE, NON_VOTING_SHARE (NULL = any)',
    has_airport_license BOOLEAN COMMENT 'Airport license requirement (NULL = any)',
    shift_type VARCHAR(20) COMMENT 'Shift type: DAY, NIGHT (NULL = any)',

    -- Metadata
    category VARCHAR(50) COMMENT 'Profile category for grouping (e.g., STANDARD, PREMIUM, SPECIAL)',
    color_code VARCHAR(10) COMMENT 'Color code for UI display (e.g., #3E5244)',
    display_order INT DEFAULT 0 COMMENT 'Order for UI display',

    -- Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether profile is available for assignment',
    is_system_profile BOOLEAN DEFAULT FALSE COMMENT 'System profiles cannot be deleted',
    usage_count INT DEFAULT 0 COMMENT 'Number of shifts assigned to this profile',

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(100) COMMENT 'User who created this profile',
    updated_by VARCHAR(100) COMMENT 'User who last updated this profile',

    INDEX idx_profile_code (profile_code),
    INDEX idx_profile_active (is_active),
    INDEX idx_profile_category (category),
    INDEX idx_profile_cab_type (cab_type),
    INDEX idx_profile_share_type (share_type),
    INDEX idx_profile_shift_type (shift_type),
    INDEX idx_profile_system (is_system_profile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Reusable shift attribute profiles for categorization and expense matching';

-- ============================================================================
-- PART 2: Create shift_profile_attribute table
-- ============================================================================
-- Join table for associating dynamic attributes with profiles
-- Allows profiles to require or exclude specific custom attributes

CREATE TABLE IF NOT EXISTS shift_profile_attribute (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique association ID',
    profile_id BIGINT NOT NULL COMMENT 'Reference to shift_profile',
    attribute_type_id BIGINT NOT NULL COMMENT 'Reference to cab_attribute_type',
    is_required BOOLEAN DEFAULT FALSE COMMENT 'true = must have attribute, false = must not have',
    expected_value VARCHAR(255) COMMENT 'Optional specific value to match (if NULL, any value acceptable)',

    CONSTRAINT fk_profile_attr_profile
        FOREIGN KEY (profile_id) REFERENCES shift_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_attr_type
        FOREIGN KEY (attribute_type_id) REFERENCES cab_attribute_type(id) ON DELETE CASCADE,

    UNIQUE KEY uk_profile_attribute (profile_id, attribute_type_id),
    INDEX idx_profile_attr_profile (profile_id),
    INDEX idx_profile_attr_type (attribute_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Dynamic attributes associated with shift profiles';

-- ============================================================================
-- PART 3: Alter cab_shift table to add profile relationship
-- ============================================================================

ALTER TABLE cab_shift
    ADD COLUMN shift_profile_id BIGINT COMMENT 'Reference to shift_profile (optional assignment)',
    ADD CONSTRAINT fk_shift_profile
        FOREIGN KEY (shift_profile_id) REFERENCES shift_profile(id) ON DELETE SET NULL,
    ADD INDEX idx_shift_profile_id (shift_profile_id);

-- ============================================================================
-- PART 4: Alter expense_category table to add profile relationship
-- ============================================================================

ALTER TABLE expense_category
    ADD COLUMN shift_profile_id BIGINT COMMENT 'Link to shift_profile for auto-expense application',
    ADD CONSTRAINT fk_expense_category_profile
        FOREIGN KEY (shift_profile_id) REFERENCES shift_profile(id) ON DELETE SET NULL,
    ADD INDEX idx_expense_category_profile (shift_profile_id);

-- ============================================================================
-- PART 5: Insert default system profiles
-- ============================================================================
-- These 8 profiles cover common operational scenarios
-- All system profiles cannot be deleted, but can be deactivated

INSERT INTO shift_profile (
    profile_code, profile_name, description,
    cab_type, share_type, has_airport_license, shift_type,
    category, color_code, display_order,
    is_active, is_system_profile, usage_count,
    created_by
) VALUES
-- Standard Sedan Profiles
('STANDARD_SEDAN_VOTING', 'Standard Sedan - Voting Share',
 'Regular sedan taxi with voting share rights',
 'SEDAN', 'VOTING_SHARE', FALSE, NULL,
 'STANDARD', '#3E5244', 1,
 TRUE, TRUE, 0, 'SYSTEM'),

('STANDARD_SEDAN_NON_VOTING', 'Standard Sedan - Non-Voting Share',
 'Regular sedan taxi with non-voting share rights',
 'SEDAN', 'NON_VOTING_SHARE', FALSE, NULL,
 'STANDARD', '#5B6B68', 2,
 TRUE, TRUE, 0, 'SYSTEM'),

-- Premium Sedan Profiles (with Airport License)
('PREMIUM_SEDAN_VOTING', 'Premium Sedan - Airport License - Voting',
 'Premium sedan with airport license and voting share rights',
 'SEDAN', 'VOTING_SHARE', TRUE, NULL,
 'PREMIUM', '#10B981', 3,
 TRUE, TRUE, 0, 'SYSTEM'),

('PREMIUM_SEDAN_NON_VOTING', 'Premium Sedan - Airport License - Non-Voting',
 'Premium sedan with airport license and non-voting share rights',
 'SEDAN', 'NON_VOTING_SHARE', TRUE, NULL,
 'PREMIUM', '#059669', 4,
 TRUE, TRUE, 0, 'SYSTEM'),

-- Handicap Van Profiles
('HANDICAP_VAN_VOTING', 'Handicap Van - Voting Share',
 'Accessible handicap van with voting share rights',
 'HANDICAP_VAN', 'VOTING_SHARE', FALSE, NULL,
 'SPECIAL', '#F59E0B', 5,
 TRUE, TRUE, 0, 'SYSTEM'),

('HANDICAP_VAN_NON_VOTING', 'Handicap Van - Non-Voting Share',
 'Accessible handicap van with non-voting share rights',
 'HANDICAP_VAN', 'NON_VOTING_SHARE', FALSE, NULL,
 'SPECIAL', '#DC2626', 6,
 TRUE, TRUE, 0, 'SYSTEM'),

-- Shift-based Profiles (Time-specific operations)
('DAY_SHIFT_SEDAN', 'Day Shift - Sedan Operations',
 'Sedan operations specifically for day shift (06:00-18:00)',
 'SEDAN', NULL, NULL, 'DAY',
 'TIME_BASED', '#60A5FA', 7,
 TRUE, TRUE, 0, 'SYSTEM'),

('NIGHT_SHIFT_SEDAN', 'Night Shift - Sedan Operations',
 'Sedan operations specifically for night shift (18:00-06:00)',
 'SEDAN', NULL, NULL, 'NIGHT',
 'TIME_BASED', '#3B82F6', 8,
 TRUE, TRUE, 0, 'SYSTEM');

-- ============================================================================
-- PART 6: Verification queries (run manually to verify migration success)
-- ============================================================================

-- Query 1: Verify shift_profile table exists and has default profiles
-- Expected: Should show 8 rows (all system profiles)
-- SELECT id, profile_code, profile_name, is_system_profile, is_active
-- FROM shift_profile
-- ORDER BY display_order;

-- Query 2: Verify foreign keys created
-- Expected: No errors when checking constraints
-- SELECT CONSTRAINT_NAME, TABLE_NAME
-- FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
-- WHERE REFERENCED_TABLE_NAME = 'shift_profile';

-- Query 3: Verify cab_shift has profile_id column and no existing assignments
-- Expected: All shift_profile_id values should be NULL
-- SELECT COUNT(*), COUNT(shift_profile_id) as assigned_count
-- FROM cab_shift;

-- Query 4: Verify expense_category has profile_id column and no existing assignments
-- Expected: All shift_profile_id values should be NULL
-- SELECT COUNT(*), COUNT(shift_profile_id) as assigned_count
-- FROM expense_category;
