# Legacy Data Import Guide

## Overview
This guide explains how to import legacy customer charge data into the FareFlow system using the new legacy tables.

## Architecture

The legacy system uses **driver_number** as the stable business key to map between legacy and current systems:

```
Legacy Charge
  └─> legacy_driver_id (DB ID from old system)
      └─> Legacy Driver Table
          └─> driver_number (e.g., "12345" for Gurpal)
              └─> Current Driver Table
                  └─> current_driver_id
```

**Key Point**: `driver_number` is the same in both systems, only database IDs differ.

## Tables

### 1. legacy_driver
Maps legacy driver IDs to driver_number (stable identifier).
Driver names are retrieved from the current Driver table, not stored here.

**Columns:**
- `id` - Legacy driver DB ID (NOT auto-increment, must match old system)
- `driver_number` - Stable business key (e.g., "12345")

### 2. legacy_account_customer  
Stores legacy customer records with original IDs.

**Columns:**
- `id` - Auto-increment (new primary key)
- `db_id` - **Original database ID from old system** (for mapping charges)
- `customer_id` - Legacy customer identifier (business key)
- `name`, `street`, `city`, `province`, `postal_code`
- `contact`, `phone`, `email`
- `credit_limit`, `date`, `notes`

### 3. legacy_customer_charge
Stores legacy charge records.

**Columns:**
- `id` - Auto-increment
- `amount` - Charge amount
- `date` - Charge date
- `payment` - Payment amount
- `driver_id` - FK to legacy_driver.id
- `customer_id` - FK to legacy_account_customer.id
- `cab_id` - **Nullable** (not used for charge attribution)
- `notes`, `type`

## Import Steps

### Step 1: Import Legacy Drivers

```sql
-- Import drivers with their original IDs and driver_number
-- Name is NOT imported - it's looked up from current Driver table
INSERT INTO legacy_driver (id, driver_number)
SELECT id, drivernumber
FROM old_system.driver;
```

**Critical**: The `id` field must preserve the original legacy driver ID.
**Note**: Driver names come from the current Driver table via driver_number lookup.

### Step 2: Import Legacy Customers

```sql
INSERT INTO legacy_account_customer (db_id, customer_id, name, street, city, province, postal_code, contact, phone, email, credit_limit, date, notes)
SELECT id, customerid, name, street, city, province, postalcode, contact, phone, email, creditlimit, date, notes
FROM old_system.accountcustomer;
```

**Note**: 
- `id` is auto-increment (new PK)
- `db_id` stores the original database ID (for charge mapping)
- `customer_id` preserves the legacy business identifier

### Step 3: Import Legacy Charges

```sql
INSERT INTO legacy_customer_charge (amount, date, payment, driver_id, customer_id, cab_id, notes, type)
SELECT 
    cc.amount, 
    cc.date, 
    cc.payment, 
    cc.driverid,   -- References legacy_driver.id
    lac.id,        -- Map old customerid → legacy_account_customer.id via db_id
    NULL,          -- Set cab_id to NULL (avoids FK failures)
    cc.notes, 
    cc.type
FROM old_system.customercharges cc
INNER JOIN legacy_account_customer lac ON cc.customerid = lac.db_id;
```

**Key mapping**: `old_system.customercharges.customerid` → `legacy_account_customer.db_id` → `legacy_account_customer.id`

### Optional: Disable FK Checks During Import

If you need to import data in a different order or have orphaned records:

```sql
SET FOREIGN_KEY_CHECKS = 0;

-- Run imports here

SET FOREIGN_KEY_CHECKS = 1;
```

## How Charge Attribution Works

When generating driver reports, the system:

1. Queries `legacy_customer_charge` for a date range
2. Joins to `legacy_driver` via `driver_id`
3. Gets `driver_number` from legacy_driver
4. Uses `driver_number` to attribute charges to the current driver

**Example:**
```
Legacy charge with driver_id = 789
  → legacy_driver.id = 789
  → legacy_driver.driver_number = "12345" (Gurpal)
  → Current driver table: driver_number = "12345"
  → Charge attributed to Gurpal in current system ✅
```

## Verification Queries

### Check driver mapping
```sql
SELECT 
    ld.id AS legacy_driver_id,
    ld.driver_number,
    d.id AS current_driver_id,
    d.driver_number AS current_driver_number,
    CONCAT(d.first_name, ' ', d.last_name) AS driver_name
FROM legacy_driver ld
LEFT JOIN driver d ON ld.driver_number = d.driver_number;
```

### Check charge counts
```sql
SELECT 
    ld.driver_number,
    CONCAT(d.first_name, ' ', d.last_name) AS driver_name,
    COUNT(*) AS charge_count,
    SUM(lcc.amount) AS total_amount
FROM legacy_customer_charge lcc
JOIN legacy_driver ld ON lcc.driver_id = ld.id
LEFT JOIN driver d ON ld.driver_number = d.driver_number
GROUP BY ld.driver_number, d.first_name, d.last_name
ORDER BY charge_count DESC;
```

### Check customer mapping
```sql
SELECT 
    lac.customer_id AS legacy_customer_id,
    lac.name,
    COUNT(lcc.id) AS charge_count
FROM legacy_account_customer lac
LEFT JOIN legacy_customer_charge lcc ON lac.id = lcc.customer_id
GROUP BY lac.id
ORDER BY charge_count DESC;
```

## Troubleshooting

### Error: "FK constraint violation on driver_id"
- **Cause**: Driver ID doesn't exist in `legacy_driver` table
- **Fix**: Import legacy drivers first (Step 1)

### Error: "FK constraint violation on customer_id"
- **Cause**: Customer ID doesn't exist in `legacy_account_customer` table
- **Fix**: Import legacy customers first (Step 2), ensure you're mapping to the correct `id` column

### Missing charges in reports
- **Cause**: Driver number mismatch between legacy and current systems
- **Fix**: Verify driver numbers match using the verification query above

### Cab reference errors
- **Cause**: `cab_id` references don't exist in current `cab` table
- **Fix**: It's OK - cab references are nullable and not used for charge attribution. Set to NULL if needed:
  ```sql
  UPDATE legacy_customer_charge SET cab_id = NULL WHERE cab_id NOT IN (SELECT id FROM cab);
  ```

## Enable Legacy System for Tenant

After importing data:

```sql
-- Enable legacy charge system for specific tenant
UPDATE fareflow.tenant_config 
SET use_legacy_charge_system = TRUE 
WHERE tenant_id = 'fareflow_mac-cabs';
```

The system will now use the legacy charge data for driver reports.
