-- Add Yellow Cabs tenant to FareFlow
-- Run this script AFTER creating the fareflow_yellow database

USE fareflow_yellow;

-- Step 1: Add tenant configuration
INSERT INTO tenant_config (
    tenant_id,
    company_name,
    taxicaller_company_id,
    taxicaller_api_key,
    taxicaller_base_url,
    created_at,
    updated_at
) VALUES (
    'yellow-cabs',
    'Yellow Cabs',
    NULL,  -- Set TaxiCaller company ID if you have one
    NULL,  -- Set TaxiCaller API key if you have one
    NULL,  -- Set TaxiCaller base URL if you have one
    NOW(),
    NOW()
);

-- Step 2: Create admin user
-- Username: admin@yellowcabs.com
-- Password: YellowCabs2026!
-- (BCrypt hash below - you can change the password after first login)

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
    'admin@yellowcabs.com',
    '$2a$10$8ZqN5YHZ1XJ9xKvV3h9.1eQZ5jJ5Y7xLH8Qf2yN6tQxHjLzKp7Woa',  -- Password: YellowCabs2026!
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

-- Verify the records were created
SELECT * FROM tenant_config WHERE tenant_id = 'yellow-cabs';
SELECT id, username, email, first_name, last_name, role, is_active FROM user WHERE username = 'admin@yellowcabs.com';

-- IMPORTANT NOTES:
-- 1. Default admin credentials:
--    Username: admin@yellowcabs.com
--    Password: YellowCabs2026!
--
-- 2. Change the password immediately after first login for security
--
-- 3. Update tenant_config with TaxiCaller details if you integrate with them
