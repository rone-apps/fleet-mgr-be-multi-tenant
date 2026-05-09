-- Grant permissions on fareflow_yellow to hpooni1 user
-- Run this as root user

-- Grant all privileges on fareflow_yellow database
GRANT ALL PRIVILEGES ON fareflow_yellow.* TO 'hpooni1'@'%';
GRANT ALL PRIVILEGES ON fareflow_yellow.* TO 'hpooni1'@'localhost';

-- Flush privileges to apply changes immediately
FLUSH PRIVILEGES;

-- Verify grants
SHOW GRANTS FOR 'hpooni1'@'%';
SHOW GRANTS FOR 'hpooni1'@'localhost';

-- Test connection (optional - run from terminal after this script)
-- mysql -u hpooni1 -p -e "USE fareflow_yellow; SELECT COUNT(*) FROM user;"
