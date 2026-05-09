-- Setup Yellow Cabs Tenant Data
-- Run this on the EXISTING fareflow_yellow database
--
-- Company mapping: yellow-cabs → fareflow_yellow
-- (Frontend maps company ID "yellow-cabs" to schema "fareflow_yellow")

USE fareflow_yellow;

-- ============================================
-- Step 1: Add tenant configuration
-- ============================================
INSERT INTO tenant_config (
    tenant_id,
    company_name,
    taxicaller_company_id,
    taxicaller_api_key,
    taxicaller_base_url,
    created_at,
    updated_at
) VALUES (
    'fareflow_yellow',  -- Database schema name (what backend uses)
    'Yellow Cabs',       -- Display name
    NULL,                -- Add TaxiCaller ID if needed
    NULL,                -- Add TaxiCaller API key if needed
    NULL,                -- Add TaxiCaller base URL if needed
    NOW(),
    NOW()
);

-- ============================================
-- Step 2: Create admin user
-- ============================================
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
    -- Password: Yellow2026!
    -- BCrypt hash - CHANGE IMMEDIATELY after first login
    '$2a$10$N9qo.uLOmn0IawKKPgEWnuR7IqPr3fKJJZ0fLvd9BZkXJhLc8wG2S',
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

-- ============================================
-- Verify setup
-- ============================================
SELECT '===================================================' as '';
SELECT 'YELLOW CABS TENANT SETUP COMPLETE' as 'Status';
SELECT '===================================================' as '';

SELECT
    tenant_id as 'Tenant ID (Schema)',
    company_name as 'Display Name',
    created_at as 'Created'
FROM tenant_config
WHERE tenant_id = 'fareflow_yellow';

SELECT
    id as 'User ID',
    username as 'Username',
    email as 'Email',
    CONCAT(first_name, ' ', last_name) as 'Name',
    role as 'Role',
    is_active as 'Active'
FROM user
WHERE username = 'admin';

-- ============================================
-- LOGIN INSTRUCTIONS
-- ============================================
SELECT '===================================================' as '';
SELECT 'LOGIN CREDENTIALS' as 'Instructions';
SELECT '===================================================' as '';

SELECT
    'Go to: https://www.smartfleets.ai' as 'Step 1',
    '' as '';

SELECT
    'Enter Company ID:' as 'Step 2',
    'yellow-cabs' as 'Value (WITH hyphen!)';

SELECT
    'Enter Username:' as 'Step 3',
    'admin' as 'Value';

SELECT
    'Enter Password:' as 'Step 4',
    'Yellow2026!' as 'Value (CHANGE AFTER LOGIN!)';

-- ============================================
-- HOW IT WORKS
-- ============================================
-- Frontend mapping (app/signin/page.js):
--   { id: 'yellow-cabs', name: 'Yellow Cabs', schema: 'fareflow_yellow' }
--
-- When user enters "yellow-cabs":
--   1. Frontend looks up the mapping
--   2. Finds schema: 'fareflow_yellow'
--   3. Sends X-Tenant-ID: fareflow_yellow to backend
--   4. Backend executes: USE `fareflow_yellow`;
--   5. User is authenticated against this database
-- ============================================
