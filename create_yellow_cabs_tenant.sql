-- ============================================
-- Yellow Cabs Tenant Setup
-- Run this on existing fareflow_yellow database
-- ============================================

USE fareflow_yellow;

-- Add tenant configuration
INSERT INTO tenant_config (
    tenant_id,
    company_name,
    taxicaller_company_id,
    taxicaller_api_key,
    taxicaller_base_url,
    created_at,
    updated_at
) VALUES (
    'fareflow_yellow',
    'Yellow Cabs',
    NULL,
    NULL,
    NULL,
    NOW(),
    NOW()
);

-- Create admin user
-- Password: Admin@2026
INSERT INTO user (
    username,
    password,
    email,
    first_name,
    last_name,
    role,
    is_active,
    phone,
    driver_id,
    created_at,
    updated_at
) VALUES (
    'admin',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'admin@yellowcabs.com',
    'Admin',
    'User',
    'ADMIN',
    1,
    NULL,
    NULL,
    NOW(),
    NOW()
);

-- Verify
SELECT 'Tenant Config:' as '';
SELECT * FROM tenant_config WHERE tenant_id = 'fareflow_yellow';

SELECT 'Admin User:' as '';
SELECT id, username, email, role, is_active FROM user WHERE username = 'admin';
