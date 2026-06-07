-- V104__Add_Default_Shift_Profile.sql
-- Add is_default flag to shift_profile table to support default profile assignment
--
-- Run manually if Flyway fails:
-- 1. DELETE FROM flyway_schema_history WHERE version = '104';
-- 2. Run the statements below
-- 3. INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
--    VALUES ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history), '104', 'Add Default Shift Profile', 'SQL', 'V104__Add_Default_Shift_Profile.sql', NULL, USER(), NOW(), 0, 1);

ALTER TABLE shift_profile
  ADD COLUMN is_default TINYINT(1) NOT NULL DEFAULT 0
  AFTER profile_name;

CREATE INDEX idx_shift_profile_is_default ON shift_profile(is_default);

-- Set the first profile (lowest ID) as default
SET @min_id = (SELECT MIN(id) FROM shift_profile);
UPDATE shift_profile
SET is_default = 1
WHERE id = @min_id;

-- Rollback:
-- ALTER TABLE shift_profile DROP INDEX idx_shift_profile_is_default;
-- ALTER TABLE shift_profile DROP COLUMN is_default;
