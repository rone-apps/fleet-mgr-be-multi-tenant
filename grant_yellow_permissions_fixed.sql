-- Grant permissions on fareflow_yellow to hpooni1 user
-- Run this as root user

-- First, check what hpooni1 users exist
SELECT User, Host FROM mysql.user WHERE User = 'hpooni1';

-- Grant to the % (wildcard) host (most common for app connections)
GRANT ALL PRIVILEGES ON `fareflow_yellow`.* TO 'hpooni1'@'%';

-- Also try other common patterns (only one will work based on how user was created)
-- Uncomment the line that matches your hpooni1 user's host:

-- GRANT ALL PRIVILEGES ON `fareflow_yellow`.* TO 'hpooni1'@'localhost';
-- GRANT ALL PRIVILEGES ON `fareflow_yellow`.* TO 'hpooni1'@'127.0.0.1';

-- Flush privileges
FLUSH PRIVILEGES;

-- Verify what permissions hpooni1 now has
SHOW GRANTS FOR 'hpooni1'@'%';

-- Test query (copy/paste this separately to test)
-- SELECT User, Host, Grant_priv FROM mysql.user WHERE User = 'hpooni1';
