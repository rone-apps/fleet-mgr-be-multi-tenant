-- Verify Yellow Cabs Admin User
-- Run this to check if admin user exists and is configured correctly

USE fareflow_yellow;

-- Check if admin user exists
SELECT 'Admin user status:' as '';
SELECT
    id,
    username,
    email,
    role,
    is_active,
    LENGTH(password) as password_length,
    SUBSTRING(password, 1, 10) as password_prefix
FROM user
WHERE username = 'admin';

-- Check tenant_config
SELECT 'Tenant config:' as '';
SELECT * FROM tenant_config WHERE tenant_id = 'fareflow_yellow';

-- If admin doesn't exist, create with known good hash
-- Uncomment these lines if you need to recreate:

-- DELETE FROM user WHERE username = 'admin';
--
-- INSERT INTO user (
--     username,
--     password,
--     email,
--     first_name,
--     last_name,
--     role,
--     is_active,
--     created_at,
--     updated_at
-- ) VALUES (
--     'admin',
--     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
--     'admin@yellowcabs.com',
--     'Yellow',
--     'Admin',
--     'ADMIN',
--     1,
--     NOW(),
--     NOW()
-- );
--
-- SELECT 'Admin user created!' as '';
-- SELECT id, username, email, role, is_active FROM user WHERE username = 'admin';
