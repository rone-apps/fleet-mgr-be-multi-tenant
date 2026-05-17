-- Remove name column from legacy_driver (if it exists)
-- Driver names are retrieved from current Driver table instead

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'legacy_driver'
      AND COLUMN_NAME = 'name'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE legacy_driver DROP COLUMN name',
    'SELECT "Column name does not exist, skipping" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Rollback:
-- ALTER TABLE legacy_driver ADD COLUMN name VARCHAR(255);
