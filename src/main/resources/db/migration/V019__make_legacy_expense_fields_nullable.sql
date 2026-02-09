-- Migration: Make Legacy Entity Fields Nullable in OneTimeExpense
-- Date: 2026-02-08
-- Database: MySQL 8.0+
-- Purpose: Support new application type system while maintaining backward compatibility
--          with legacy entity type system

-- ============================================================================
-- STEP 1: Make entity_id and entity_type nullable
-- ============================================================================
-- These columns are only used by the legacy entity type system
-- New application type system does not require them

ALTER TABLE one_time_expense
  MODIFY COLUMN entity_id BIGINT DEFAULT NULL COMMENT 'Legacy: ID of associated entity (for backward compatibility)',
  MODIFY COLUMN entity_type VARCHAR(20) DEFAULT NULL COMMENT 'Legacy: Type of associated entity (for backward compatibility)';

-- ============================================================================
-- NOTE: Going forward, new one-time expenses should use:
-- - application_type, shift_profile_id, specific_shift_id, specific_owner_id, specific_driver_id
--
-- Legacy system (entity_type, entity_id) is maintained for backward compatibility
-- ============================================================================
