-- Migration: Add session_history and session_count to driver_shifts
-- Date: 2026-03-10
-- Purpose: Store consolidated shift session data (multiple logon/logoff pairs) in a dedicated JSON column

ALTER TABLE driver_shifts
  ADD COLUMN session_count INT DEFAULT 1 COMMENT 'Number of logon/logoff sessions consolidated into this shift',
  ADD COLUMN session_history JSON DEFAULT NULL COMMENT 'JSON array of individual sessions: [{logon, logoff, hours}, ...]';

-- Migrate existing session data from notes field to session_history
UPDATE driver_shifts
SET session_history = SUBSTRING(notes, 11),
    session_count = JSON_LENGTH(SUBSTRING(notes, 11))
WHERE notes LIKE 'SESSIONS: [%';

-- Clear the SESSIONS prefix from notes for migrated rows
UPDATE driver_shifts
SET notes = NULL
WHERE notes LIKE 'SESSIONS: [%';
