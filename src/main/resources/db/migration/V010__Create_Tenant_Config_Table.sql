-- Tenant configuration table for storing tenant-specific settings
CREATE TABLE IF NOT EXISTS tenant_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(63) NOT NULL UNIQUE,
    company_name VARCHAR(100),
    
    -- TaxiCaller API Configuration
    taxicaller_api_key VARCHAR(100),
    taxicaller_company_id INT,
    taxicaller_base_url VARCHAR(255) DEFAULT 'https://api.taxicaller.net',
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_tenant_config_tenant_id (tenant_id)
);

-- Insert config for existing tenants (update with actual values)
INSERT INTO tenant_config (tenant_id, company_name, taxicaller_api_key, taxicaller_company_id)
VALUES 
    ('fareflow', 'FareFlow Default', '5658f7a37c72163a41855005b23add80', 42259)
ON DUPLICATE KEY UPDATE tenant_id = tenant_id;
