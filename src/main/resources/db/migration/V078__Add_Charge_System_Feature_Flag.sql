-- Add feature flag for charge system selection
-- Allows tenants to choose between legacy_customer_charge and account_charge systems

ALTER TABLE tenant_config
ADD COLUMN use_legacy_charge_system BOOLEAN NOT NULL DEFAULT FALSE
COMMENT 'Feature flag: true = legacy_customer_charge table, false = account_charge table';

-- For existing tenants, default to modern system (safe default)
UPDATE tenant_config SET use_legacy_charge_system = FALSE;

-- Example: Enable legacy system for specific tenant
-- UPDATE tenant_config SET use_legacy_charge_system = TRUE WHERE tenant_id = 'fareflow_mac-cabs';

-- Rollback:
-- ALTER TABLE tenant_config DROP COLUMN use_legacy_charge_system;
