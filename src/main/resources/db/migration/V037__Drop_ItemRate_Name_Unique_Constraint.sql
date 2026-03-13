-- Drop unique constraint on item_rate.name to allow versioning
-- Multiple rows with same name can exist (old closed versions + one active version)
ALTER TABLE item_rate DROP INDEX `name`;
