-- FINAL FIX: Create Yellow Cabs Admin User
-- This uses a Java Spring Security compatible BCrypt hash
-- Password will be: Admin@2026

USE fareflow_yellow;

-- Remove any existing admin user
DELETE FROM user WHERE username = 'admin';

-- Create admin with Spring Security BCrypt hash
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
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@yellowcabs.com',
    'Yellow',
    'Admin',
    'ADMIN',
    1,
    NULL,
    NULL,
    NOW(),
    NOW()
);

-- Verify the user was created
SELECT 'Yellow Cabs Admin User Created!' as '';
SELECT
    id,
    username,
    email,
    first_name,
    last_name,
    role,
    is_active,
    created_at
FROM user
WHERE username = 'admin';

-- Also check/create tenant_config if needed
SELECT 'Checking tenant_config...' as '';
SELECT * FROM tenant_config WHERE tenant_id = 'fareflow_yellow';

-- If tenant_config doesn't exist, uncomment and run:
/*
INSERT INTO tenant_config (tenant_id, tenant_name, database_name, is_active, created_at, updated_at)
VALUES ('fareflow_yellow', 'Yellow Cabs', 'fareflow_yellow', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    tenant_name = 'Yellow Cabs',
    is_active = 1,
    updated_at = NOW();
*/
