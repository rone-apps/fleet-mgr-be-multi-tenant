-- Make entity_type and entity_id nullable to support new ApplicationType system
-- Legacy data still works with entity_type/entity_id, but new revenues use ApplicationType

ALTER TABLE other_revenue
MODIFY COLUMN entity_type VARCHAR(30) DEFAULT NULL,
MODIFY COLUMN entity_id BIGINT DEFAULT NULL;
