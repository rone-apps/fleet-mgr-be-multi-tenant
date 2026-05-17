# DB Import UI - Legacy Data Migration Guide

## Overview
Use the FareFlow DB Import UI to migrate legacy data from your old database to the new legacy tables.

## Prerequisites
1. Run Flyway migrations (V081-V086) - start your backend:
   ```bash
   ./gradlew bootRun
   ```

2. Verify migrations completed:
   ```sql
   SELECT version FROM flyway_schema_history WHERE version >= 81 ORDER BY version;
   ```
   Should show V081, V082, V083, V084, V085, V086

## Database Structure After Migrations

### legacy_driver
- `id` (PK) - Original driver ID from old system
- `driver_number` - Stable business key

### legacy_account_customer
- `id` (PK, auto-increment) - New ID (not used for import mapping)
- `db_id` (UNIQUE) - **Original customer ID from old system** (used for FK)
- `customer_id` - Business identifier (e.g., "CUST001")
- Other fields: name, street, city, etc.

### legacy_customer_charge
- `id` (PK, auto-increment)
- `driver_id` (FK → `legacy_driver.id`)
- `customer_id` (FK → `legacy_account_customer.db_id`) ⚠️ Note: references db_id, not id!
- `cab_id` (FK → `cab.id`, nullable)
- Other fields: amount, date, payment, notes, type

## Step-by-Step Import Process

### Step 1: Import Drivers

**Source:** `old_system.driver`  
**Destination:** `legacy_driver`

**Column Mapping:**
```
id           → id
drivernumber → driver_number
```

**Important:**
- Do NOT map `name` - the legacy_driver table doesn't have this column
- The `id` column is NOT auto-increment - it preserves the original ID

**Conflict Resolution:** Skip duplicates (INSERT IGNORE)

**Expected Result:** All drivers imported with original IDs preserved

---

### Step 2: Import Customers

**Source:** `old_system.accountcustomer`  
**Destination:** `legacy_account_customer`

**Column Mapping:**
```
id          → db_id          (Original DB ID - critical for charge mapping!)
customerid  → customer_id     (Business identifier)
name        → name
street      → street
city        → city
province    → province
postalcode  → postal_code
contact     → contact
phone       → phone
email       → email
creditlimit → credit_limit
date        → date
notes       → notes
```

**Important:**
- Map `id` → `db_id` (NOT to `id`)
- The `id` column in `legacy_account_customer` is auto-generated
- `db_id` stores the original database ID for charge mapping

**Conflict Resolution:** Skip duplicates on `db_id` (INSERT IGNORE)

**Verification:**
```sql
SELECT COUNT(*) FROM legacy_account_customer;
SELECT id, db_id, customer_id, name FROM legacy_account_customer LIMIT 10;
```

---

### Step 3: Import Charges

**Source:** `old_system.customercharges`  
**Destination:** `legacy_customer_charge`

**Column Mapping:**
```
amount      → amount
date        → date
payment     → payment
driverid    → driver_id      (FK to legacy_driver.id)
customerid  → customer_id    (FK to legacy_account_customer.db_id)
notes       → notes
type        → type
cabid       → (skip - leave NULL)
```

**Important:**
- `customerid` → `customer_id`: Direct mapping works because FK now points to `db_id`
- `cabid`: Skip this field or map to NULL (avoids FK constraint failures)
- `driver_id`: Direct mapping (old driver IDs are preserved in legacy_driver)

**Conflict Resolution:** Skip duplicates (INSERT IGNORE)

**Expected Behavior:**
- ✅ Charges with valid driver_id and customer_id will import
- ⚠️ Charges with missing drivers will be skipped (FK constraint)
- ⚠️ Charges with missing customers will be skipped (FK constraint)

**Verification:**
```sql
-- Check total charges imported
SELECT COUNT(*) FROM legacy_customer_charge;

-- Check charges with customer details
SELECT 
    lcc.id,
    lcc.amount,
    lcc.date,
    lac.customer_id,
    lac.name AS customer_name,
    ld.driver_number
FROM legacy_customer_charge lcc
JOIN legacy_account_customer lac ON lcc.customer_id = lac.db_id
JOIN legacy_driver ld ON lcc.driver_id = ld.id
LIMIT 10;
```

---

## Common Issues & Solutions

### Issue 1: "Skipped duplicate" messages

**Cause:** FK constraint violation (driver or customer doesn't exist)

**Solution:**
1. Check if drivers were imported:
   ```sql
   SELECT COUNT(*) FROM legacy_driver;
   ```

2. Check if customers were imported with `db_id`:
   ```sql
   SELECT COUNT(*) FROM legacy_account_customer WHERE db_id IS NOT NULL;
   ```

3. Find which customer IDs are missing:
   ```sql
   SELECT DISTINCT customerid 
   FROM old_system.customercharges 
   WHERE customerid NOT IN (SELECT db_id FROM legacy_account_customer);
   ```

### Issue 2: "customer_id cannot be NULL" error

**Cause:** Trying to import charges before importing customers

**Solution:** Import in order:
1. legacy_driver (Step 1)
2. legacy_account_customer (Step 2)
3. legacy_customer_charge (Step 3)

### Issue 3: FK constraint error on customer_id

**Cause:** Mapped `id` instead of `db_id` in Step 2

**Solution:**
```sql
-- Check if db_id is populated
SELECT COUNT(*) FROM legacy_account_customer WHERE db_id IS NULL;

-- If NULL, re-import with correct mapping:
TRUNCATE legacy_account_customer;
-- Then re-run Step 2 with id → db_id mapping
```

### Issue 4: All charges skipped

**Cause:** Column mapping is incorrect

**Solution:** Verify mappings in DB Import UI:
- `driverid` → `driver_id` (not `id`)
- `customerid` → `customer_id` (not `db_id`)

---

## Final Verification

After all imports complete, run these checks:

### Check 1: Driver mapping
```sql
SELECT 
    ld.id AS legacy_id,
    ld.driver_number,
    d.id AS current_id,
    CONCAT(d.first_name, ' ', d.last_name) AS driver_name
FROM legacy_driver ld
LEFT JOIN driver d ON ld.driver_number = d.driver_number
LIMIT 10;
```

**Expected:** All legacy drivers should match current drivers via `driver_number`

### Check 2: Customer counts
```sql
SELECT 
    COUNT(*) AS total_customers,
    COUNT(db_id) AS customers_with_db_id
FROM legacy_account_customer;
```

**Expected:** Both counts should be equal

### Check 3: Charge counts and totals
```sql
SELECT 
    COUNT(*) AS total_charges,
    COUNT(DISTINCT customer_id) AS unique_customers,
    COUNT(DISTINCT driver_id) AS unique_drivers,
    SUM(amount) AS total_amount
FROM legacy_customer_charge;
```

### Check 4: Full join test
```sql
SELECT 
    COUNT(*) AS charges_with_full_details
FROM legacy_customer_charge lcc
JOIN legacy_account_customer lac ON lcc.customer_id = lac.db_id
JOIN legacy_driver ld ON lcc.driver_id = ld.id
JOIN driver d ON ld.driver_number = d.driver_number;
```

**Expected:** Count should match total charges (all charges should have valid references)

---

## Enable Legacy System

After successful import:

```sql
UPDATE fareflow.tenant_config 
SET use_legacy_charge_system = TRUE 
WHERE tenant_id = 'fareflow_YOUR-TENANT';
```

Verify it's enabled:
```sql
SELECT tenant_id, use_legacy_charge_system 
FROM fareflow.tenant_config;
```

---

## Troubleshooting Query

If charges aren't appearing in driver reports:

```sql
-- Check if charges exist for a specific driver_number
SELECT 
    lcc.*,
    lac.name AS customer_name,
    ld.driver_number
FROM legacy_customer_charge lcc
JOIN legacy_driver ld ON lcc.driver_id = ld.id
JOIN legacy_account_customer lac ON lcc.customer_id = lac.db_id
WHERE ld.driver_number = 'YOUR-DRIVER-NUMBER'
ORDER BY lcc.date DESC;
```

---

## Summary

**Key Points:**
1. ✅ `legacy_driver.id` = Original driver ID (preserved)
2. ✅ `legacy_account_customer.db_id` = Original customer ID (preserved)
3. ✅ `legacy_customer_charge.customer_id` references `db_id` (not `id`)
4. ✅ Direct column mapping works in DB Import UI (no JOINs needed)
5. ⚠️ Import order matters: drivers → customers → charges
6. ⚠️ Map `id` → `db_id` for customers (NOT `id` → `id`)

**The FK relationships:**
```
legacy_customer_charge.driver_id   → legacy_driver.id
legacy_customer_charge.customer_id → legacy_account_customer.db_id
```

This structure allows the DB Import UI to work with simple column mapping!
