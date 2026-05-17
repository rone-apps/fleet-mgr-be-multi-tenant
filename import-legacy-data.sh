#!/bin/bash

# Legacy Data Import Script
# Run this after Flyway migrations have completed

set -e  # Exit on error

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-fareflow}"
DB_USER="${DB_USER:-root}"
OLD_DB="${OLD_DB:-old_system}"  # Your old database name

echo "========================================="
echo "Legacy Data Import for FareFlow"
echo "========================================="
echo ""
echo "Target Database: $DB_NAME"
echo "Source Database: $OLD_DB"
echo ""

# Prompt for password
read -sp "Enter MySQL password for $DB_USER: " DB_PASS
echo ""

# Function to run SQL
run_sql() {
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "$1"
}

# Step 1: Import legacy drivers
echo ""
echo "Step 1: Importing legacy drivers..."
run_sql "
INSERT INTO legacy_driver (id, driver_number)
SELECT id, drivernumber
FROM ${OLD_DB}.driver
ON DUPLICATE KEY UPDATE driver_number = VALUES(driver_number);
"
echo "✓ Legacy drivers imported"

# Step 2: Import legacy customers
echo ""
echo "Step 2: Importing legacy customers..."
run_sql "
INSERT INTO legacy_account_customer (db_id, customer_id, name, street, city, province, postal_code, contact, phone, email, credit_limit, date, notes)
SELECT id, customerid, name, street, city, province, postalcode, contact, phone, email, creditlimit, date, notes
FROM ${OLD_DB}.accountcustomer
ON DUPLICATE KEY UPDATE name = VALUES(name);
"
echo "✓ Legacy customers imported"

# Step 3: Import legacy charges
echo ""
echo "Step 3: Importing legacy charges..."
run_sql "
INSERT INTO legacy_customer_charge (amount, date, payment, driver_id, customer_id, cab_id, notes, type)
SELECT
    cc.amount,
    cc.date,
    cc.payment,
    cc.driverid,
    lac.id,  -- Map via db_id: old customerid → lac.db_id → lac.id
    NULL,    -- Set cab_id to NULL (avoids FK failures)
    cc.notes,
    cc.type
FROM ${OLD_DB}.customercharges cc
INNER JOIN legacy_account_customer lac ON cc.customerid = lac.db_id
WHERE cc.driverid IN (SELECT id FROM legacy_driver);
"
echo "✓ Legacy charges imported"

# Step 4: Verification
echo ""
echo "========================================="
echo "Verification"
echo "========================================="

echo ""
echo "Legacy Drivers:"
run_sql "SELECT COUNT(*) AS count FROM legacy_driver;" | tail -1

echo ""
echo "Legacy Customers:"
run_sql "SELECT COUNT(*) AS count FROM legacy_account_customer;" | tail -1

echo ""
echo "Legacy Charges:"
run_sql "SELECT COUNT(*) AS count FROM legacy_customer_charge;" | tail -1

echo ""
echo "Driver Mapping Check:"
run_sql "
SELECT
    COUNT(*) AS mapped_drivers,
    SUM(CASE WHEN d.id IS NULL THEN 1 ELSE 0 END) AS unmapped_drivers
FROM legacy_driver ld
LEFT JOIN driver d ON ld.driver_number = d.driver_number;
" | tail -1

echo ""
echo "========================================="
echo "Import Complete!"
echo "========================================="
echo ""
echo "Next step: Enable legacy system for your tenant"
echo "Run this SQL:"
echo ""
echo "UPDATE fareflow.tenant_config"
echo "SET use_legacy_charge_system = TRUE"
echo "WHERE tenant_id = 'fareflow_YOUR-TENANT';"
echo ""
