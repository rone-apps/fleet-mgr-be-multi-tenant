-- Setup Yellow Cabs Tenant for FareFlow Multi-Tenant System
-- Following the existing naming convention: fareflow_*
--
-- Existing tenants:
--   - fareflow (default)
--   - fareflow_bonny (Bonny's Taxi)
--   - fareflow_demo (Demo)
-- New tenant:
--   - fareflow_yellow (Yellow Cabs)

-- ============================================
-- Step 1: Create the database
-- ============================================
CREATE DATABASE IF NOT EXISTS `fareflow_yellow`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `fareflow_yellow`;

-- ============================================
-- Step 2: Create all tables
-- ============================================
-- INSTRUCTIONS:
-- 1. Update create_fareflow_database.sql:
--    Line 4: CREATE DATABASE IF NOT EXISTS `fareflow_yellow`
--    Line 8: USE `fareflow_yellow`;
-- 2. Run the entire create_fareflow_database.sql script
-- 3. Then come back and run the rest of this script (Steps 3-5)

-- ============================================
-- Step 3: Add tenant configuration
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
    'fareflow_yellow',  -- Company ID - must match database name
    'Yellow Cabs',      -- Display name
    NULL,               -- Add TaxiCaller ID if integrating
    NULL,               -- Add TaxiCaller API key if integrating
    NULL,               -- Add TaxiCaller base URL if integrating
    NOW(),
    NOW()
);

-- ============================================
-- Step 4: Create admin user
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
    -- Default password: Yellow2026!
    -- BCrypt hash for "Yellow2026!" - CHANGE AFTER FIRST LOGIN!
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
-- Step 5: Verify setup
-- ============================================
SELECT '========================================' as '';
SELECT 'TENANT CONFIGURATION' as 'Verification';
SELECT '========================================' as '';

SELECT
    tenant_id as 'Company ID',
    company_name as 'Company Name',
    created_at as 'Created'
FROM tenant_config
WHERE tenant_id = 'fareflow_yellow';

SELECT '========================================' as '';
SELECT 'ADMIN USER' as 'Verification';
SELECT '========================================' as '';

SELECT
    id as 'User ID',
    username as 'Username',
    email as 'Email',
    CONCAT(first_name, ' ', last_name) as 'Full Name',
    role as 'Role',
    is_active as 'Active'
FROM user
WHERE username = 'admin';

SELECT '========================================' as '';
SELECT 'TABLE COUNT' as 'Verification';
SELECT '========================================' as '';

SELECT COUNT(*) as 'Total Tables' FROM information_schema.tables
WHERE table_schema = 'fareflow_yellow';

-- ============================================
-- LOGIN INSTRUCTIONS
-- ============================================
SELECT '========================================' as '';
SELECT 'LOGIN CREDENTIALS' as 'Instructions';
SELECT '========================================' as '';

SELECT
    'fareflow_yellow' as 'Company ID',
    'admin' as 'Username',
    'Yellow2026!' as 'Password (CHANGE IMMEDIATELY)',
    'https://www.smartfleets.ai' as 'Login URL';

-- ============================================
-- NOTES
-- ============================================
-- When logging into Smart Fleets app:
--
-- 1. Go to: https://www.smartfleets.ai
-- 2. Enter Company ID:  fareflow_yellow
-- 3. Enter Username:    admin
-- 4. Enter Password:    Yellow2026!
-- 5. Change password immediately after first login
--
-- The application will automatically route to 'fareflow_yellow' database.
--
-- To add more users, roles, cabs, drivers, etc., log in as admin
-- and use the application's user management features.
-- ============================================
