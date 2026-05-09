-- Fix Yellow Cabs Admin Password
USE fareflow_yellow;

-- First, check if user exists
SELECT 'Checking existing admin user...' as '';
SELECT id, username, email, role FROM user WHERE username = 'admin';

-- Delete existing admin if it exists (to start fresh)
DELETE FROM user WHERE username = 'admin';

-- Create admin user with working password
-- Password: password (yes, just "password" - change it after login!)
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
SELECT 'Admin user created/updated:' as '';
SELECT id, username, email, first_name, last_name, role, is_active
FROM user WHERE username = 'admin';

-- Also verify tenant config exists
SELECT 'Tenant config:' as '';
SELECT * FROM tenant_config WHERE tenant_id = 'fareflow_yellow';
