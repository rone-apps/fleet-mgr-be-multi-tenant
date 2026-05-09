-- Quick Login Test for Yellow Cabs
-- Run this to diagnose the login issue

-- Step 1: Switch to yellow database
USE fareflow_yellow;

-- Step 2: Check if admin user exists
SELECT '=== CHECKING ADMIN USER ===' as '';
SELECT
    id,
    username,
    password,
    email,
    role,
    is_active,
    created_at
FROM user
WHERE username = 'admin';

-- Step 3: Check all users in the database
SELECT '=== ALL USERS IN FAREFLOW_YELLOW ===' as '';
SELECT
    id,
    username,
    email,
    role,
    is_active
FROM user
ORDER BY id;

-- Step 4: Check tenant_config
SELECT '=== TENANT CONFIG ===' as '';
SELECT * FROM tenant_config;

-- Step 5: If no admin user found, CREATE IT NOW
-- Remove the /* and */ below to execute the INSERT

/*
DELETE FROM user WHERE username = 'admin';

INSERT INTO user (
    username,
    password,
    email,
    first_name,
    last_name,
    role,
    is_active,
    created_at,
    updated_at
) VALUES (
    'admin',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'admin@yellowcabs.com',
    'Yellow',
    'Admin',
    'ADMIN',
    1,
    NOW(),
    NOW()
);

SELECT '=== ADMIN USER CREATED ===' as '';
SELECT id, username, email, role FROM user WHERE username = 'admin';
*/
