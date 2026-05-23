-- V100__Create_Plugin_Tables.sql
-- Creates tables for the plugin framework
-- Part of multi-tenant plugin architecture implementation

-- =============================================================================
-- SHARED CONFIGURATION (in 'fareflow' schema)
-- =============================================================================

-- Plugin configuration table (shared across tenants)
CREATE TABLE IF NOT EXISTS fareflow.plugin_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(63) NOT NULL COMMENT 'Tenant this configuration belongs to',
    plugin_id VARCHAR(50) NOT NULL COMMENT 'Plugin identifier (e.g., taxicaller, icabbi, moneris)',

    config_data JSON NOT NULL COMMENT 'Plugin-specific configuration (API keys, endpoints, etc.)',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether plugin is enabled for this tenant',
    schedule_config JSON COMMENT 'Scheduling configuration (cron, frequency, etc.)',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    UNIQUE KEY uk_plugin_tenant (tenant_id, plugin_id),
    INDEX idx_plugin_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Plugin configuration storage (shared in fareflow schema)';

-- =============================================================================
-- PER-TENANT TABLES (in each tenant schema)
-- =============================================================================

-- Plugin installation table (per-tenant)
-- This tracks which plugins are installed/activated for a tenant
CREATE TABLE IF NOT EXISTS plugin_installation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plugin_id VARCHAR(50) NOT NULL COMMENT 'Plugin identifier',

    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether this installation is active',
    version VARCHAR(20) COMMENT 'Installed plugin version',
    instance_config JSON COMMENT 'Instance-specific config (e.g., per-cab credentials for Moneris)',

    installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_executed_at TIMESTAMP NULL COMMENT 'Last successful execution timestamp',
    last_execution_status VARCHAR(20) COMMENT 'Last execution status: SUCCESS, FAILED, PARTIAL',

    INDEX idx_plugin_active (plugin_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Plugin installation tracking (per-tenant schema)';

-- Plugin execution history table (per-tenant)
-- Audit log of all plugin executions
CREATE TABLE IF NOT EXISTS plugin_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plugin_id VARCHAR(50) NOT NULL COMMENT 'Plugin identifier',

    execution_type VARCHAR(30) NOT NULL COMMENT 'Execution trigger: SCHEDULED, MANUAL, WEBHOOK',
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    status VARCHAR(20) COMMENT 'Execution status: SUCCESS, FAILED, PARTIAL, RUNNING',

    records_processed INT NOT NULL DEFAULT 0 COMMENT 'Total records attempted',
    records_success INT NOT NULL DEFAULT 0 COMMENT 'Successfully processed records',
    records_failed INT NOT NULL DEFAULT 0 COMMENT 'Failed records',

    error_message TEXT COMMENT 'Error details if execution failed',
    execution_metadata JSON COMMENT 'Execution parameters (date ranges, filters, etc.)',

    created_by VARCHAR(100) COMMENT 'User or system that triggered execution',

    INDEX idx_plugin_exec_time (plugin_id, start_time),
    INDEX idx_plugin_exec_status (plugin_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Plugin execution audit log (per-tenant schema)';

-- =============================================================================
-- ROLLBACK INSTRUCTIONS
-- =============================================================================

-- To rollback this migration (if needed):
-- DROP TABLE IF EXISTS plugin_execution;
-- DROP TABLE IF EXISTS plugin_installation;
-- DROP TABLE IF EXISTS fareflow.plugin_config;
