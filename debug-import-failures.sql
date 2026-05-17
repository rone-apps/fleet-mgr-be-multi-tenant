-- Debug script to find why charges are being skipped

-- 1. Check FK constraints on legacy_customer_charge
SELECT
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'legacy_customer_charge'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 2. Check if drivers exist (using sample IDs from your error)
-- Replace with actual driver IDs from your failed rows
SELECT 'Checking drivers...' AS step;
SELECT COUNT(*) AS drivers_exist FROM legacy_driver WHERE id IN (
    -- Add driver_id values from your failed charges here
    SELECT DISTINCT driverid FROM old_system.customercharges LIMIT 10
);

-- 3. Check if customers exist in legacy_account_customer (via db_id)
SELECT 'Checking customers...' AS step;
SELECT
    'Customer 429872' AS customer,
    CASE WHEN EXISTS(SELECT 1 FROM legacy_account_customer WHERE db_id = 429872)
         THEN 'EXISTS' ELSE 'MISSING' END AS status
UNION ALL
SELECT
    'Customer 429936',
    CASE WHEN EXISTS(SELECT 1 FROM legacy_account_customer WHERE db_id = 429936)
         THEN 'EXISTS' ELSE 'MISSING' END;

-- 4. Check if cabs exist (this is likely the problem)
SELECT 'Checking cabs...' AS step;
SELECT
    'Cab 3427500' AS cab,
    CASE WHEN EXISTS(SELECT 1 FROM cab WHERE id = 3427500)
         THEN 'EXISTS' ELSE 'MISSING' END AS status
UNION ALL
SELECT
    'Cab 223369',
    CASE WHEN EXISTS(SELECT 1 FROM cab WHERE id = 223369)
         THEN 'EXISTS' ELSE 'MISSING' END
UNION ALL
SELECT
    'Cab 223344',
    CASE WHEN EXISTS(SELECT 1 FROM cab WHERE id = 223344)
         THEN 'EXISTS' ELSE 'MISSING' END;

-- 5. Show sample of what would fail
SELECT 'Sample of charges that will fail:' AS step;
SELECT
    cc.driverid,
    cc.customerid,
    cc.cabid,
    CASE WHEN ld.id IS NULL THEN 'MISSING DRIVER' ELSE 'OK' END AS driver_status,
    CASE WHEN lac.db_id IS NULL THEN 'MISSING CUSTOMER' ELSE 'OK' END AS customer_status,
    CASE WHEN cc.cabid IS NULL THEN 'NULL'
         WHEN c.id IS NULL THEN 'MISSING CAB'
         ELSE 'OK' END AS cab_status
FROM old_system.customercharges cc
LEFT JOIN legacy_driver ld ON cc.driverid = ld.id
LEFT JOIN legacy_account_customer lac ON cc.customerid = lac.db_id
LEFT JOIN cab c ON cc.cabid = c.id
LIMIT 20;
