-- V112__Allow_Nullable_Revenue_Fields_For_ApplicationType.sql
-- Allow NULL values for entity_type, entity_id, and revenue_category_id
-- when using the new ApplicationType system for standalone revenues

-- Make entity_type nullable (new ApplicationType system doesn't require legacy entity_type)
ALTER TABLE other_revenue
MODIFY COLUMN entity_type VARCHAR(20) NULL;

-- Make entity_id nullable (new ApplicationType system doesn't require legacy entity_id)
ALTER TABLE other_revenue
MODIFY COLUMN entity_id BIGINT NULL;

-- Make revenue_category_id nullable (standalone revenues don't need a category)
ALTER TABLE other_revenue
MODIFY COLUMN revenue_category_id BIGINT NULL;

-- Rollback:
-- ALTER TABLE other_revenue MODIFY COLUMN entity_type VARCHAR(20) NOT NULL;
-- ALTER TABLE other_revenue MODIFY COLUMN entity_id BIGINT NOT NULL;
-- ALTER TABLE other_revenue MODIFY COLUMN revenue_category_id BIGINT NOT NULL;
