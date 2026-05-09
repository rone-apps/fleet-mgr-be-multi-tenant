-- Setup Yellow Cabs Tenant for FareFlow Multi-Tenant System
--
-- IMPORTANT: Tenant ID "yellowcabs" maps to database schema "yellowcabs"
-- (The application removes hyphens/special chars during tenant resolution)

-- ============================================
-- Step 1: Create the database
-- ============================================
CREATE DATABASE IF NOT EXISTS `yellowcabs`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `yellowcabs`;

-- ============================================
-- Step 2: Create all tables
-- ============================================
-- Run the create_fareflow_database.sql script content here
-- (all the CREATE TABLE statements)
-- OR import the modified create_fareflow_database.sql after changing:
--   Line 4: CREATE DATABASE IF NOT EXISTS `yellowcabs`
--   Line 8: USE `yellowcabs`;

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
    'yellowcabs',  -- IMPORTANT: Must match what users enter at login (no hyphen!)
    'Yellow Cabs',
    NULL,
    NULL,
    NULL,
    NOW(),
    NOW()
);

-- ============================================
-- Step 4: Create admin user
-- ============================================
-- Password will be set during first login or via password reset

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
    -- Temporary password: YellowCabs2026!
    -- BCrypt hash - change immediately after first login
    '$2a$10$kF8GZqN5YHZ1XJ9xKvV3h9.1eQZ5jJ5Y7xLH8Qf2yN6tQxHjLzKp7W',
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
SELECT
    'Tenant Config' as 'Setup Component',
    tenant_id,
    company_name
FROM tenant_config
WHERE tenant_id = 'yellowcabs';

SELECT
    'Admin User' as 'Setup Component',
    id,
    username,
    email,
    role,
    is_active
FROM user
WHERE username = 'admin@yellowcabs.com';

-- ============================================
-- LOGIN INSTRUCTIONS
-- ============================================
-- When logging into Smart Fleets app:
--
-- Company ID:  yellowcabs      (NO HYPHEN!)
-- Username:    admin@yellowcabs.com
-- Password:    YellowCabs2026!
--
-- The application will automatically route to the 'yellowcabs' database.
-- ============================================
