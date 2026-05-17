# Legacy Charge System - Complete Data Flow

## Overview
This document explains how legacy customer charges flow through the system for driver reports.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      DRIVER REPORT REQUEST                       │
│                   (e.g., Monthly Summary for Gurpal)             │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│           FinancialStatementService / ReportService              │
│                                                                   │
│  • Receives: person_id (e.g., 123) for Gurpal                   │
│  • Needs: Customer charge data for date range                    │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              CustomerChargeProviderFactory                       │
│                                                                   │
│  Checks: TenantConfig.useLegacyChargeSystem                      │
│  • TRUE  → Returns LegacyCustomerChargeProvider                  │
│  • FALSE → Returns ModernAccountChargeProvider                   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    ┌───────────┴──────────┐
                    ▼                      ▼
        ┌──────────────────┐    ┌──────────────────┐
        │   LEGACY PATH    │    │   MODERN PATH    │
        └──────────────────┘    └──────────────────┘
                │                        │
                ▼                        ▼
    LegacyCustomerChargeProvider    ModernAccountChargeProvider
                │                        │
                │                        │
                ▼                        ▼
```

## Legacy Path - Detailed Flow

### Step 1: Receive Current Driver ID

```java
// FinancialStatementService calls:
provider.findChargesByDriverId(personId, startDate, endDate);
// Example: personId = 123 (Gurpal's person ID in current system)
```

### Step 2: Map to Driver Number (Stable Business Key)

```java
// LegacyCustomerChargeProvider:
Driver currentDriver = driverRepository.findByPersonId(123);
String driverNumber = currentDriver.getDriverNumber();  // "12345"
```

**Current System:**
```
person_id = 123  →  Driver table  →  driver_number = "12345"
```

### Step 3: Query Legacy Charges via Driver Number

```sql
-- Repository Query (JPQL):
SELECT lcc FROM LegacyCustomerCharge lcc
WHERE lcc.driver.driverNumber = '12345'
AND lcc.date BETWEEN :startDate AND :endDate
ORDER BY lcc.date

-- Translates to SQL joins:
SELECT lcc.*
FROM legacy_customer_charge lcc
JOIN legacy_driver ld ON lcc.driver_id = ld.id
WHERE ld.driver_number = '12345'
  AND lcc.date BETWEEN ? AND ?
ORDER BY lcc.date
```

**Legacy System:**
```
driver_number = "12345"  →  legacy_driver table  →  id = 789
                                                   ↓
                                        legacy_customer_charge
                                        driver_id = 789
```

### Step 4: Return Normalized DTOs

```java
// Each LegacyCustomerCharge is mapped to CustomerChargeDTO
CustomerChargeDTO {
    id: 456,
    chargeDate: 2026-05-10,
    customerName: "ABC Company",
    driverNumber: "12345",  // From legacy_driver.driver_number
    driverName: "Gurpal Singh",  // From legacy_driver.name
    fareAmount: 45.00,
    totalAmount: 45.00,
    sourceSystem: "LEGACY"
}
```

### Step 5: Display in Report

The report services receive normalized DTOs and don't know/care whether the data came from legacy or modern system.

```
Driver Monthly Report - Gurpal Singh (12345)
─────────────────────────────────────────────
Account Charges:
  2026-05-10  ABC Company      $45.00
  2026-05-12  XYZ Corp         $32.50
  2026-05-15  DEF Ltd          $67.00
                               ───────
  Total Charges:              $144.50
```

## Database Schema Relationships

### Legacy Tables

```
┌─────────────────┐
│ legacy_driver   │
├─────────────────┤
│ id (PK)         │ ← Legacy driver ID from old system
│ driver_number   │ ← Stable business key (same as current system)
│ name            │
└─────────────────┘
        ▲
        │ FK: driver_id
        │
┌──────────────────────┐
│ legacy_customer_     │
│ charge               │
├──────────────────────┤
│ id (PK)              │
│ amount               │
│ date                 │
│ payment              │
│ driver_id (FK) ──────┘
│ customer_id (FK)     │
│ cab_id (nullable)    │
│ notes                │
│ type                 │
└──────────────────────┘
```

### Current Tables

```
┌─────────────┐
│ person      │
├─────────────┤
│ id (PK)     │ ← Current person_id (e.g., 123)
└─────────────┘
      ▲
      │
┌─────────────┐
│ driver      │
├─────────────┤
│ person_id   │
│ driver_     │
│ number      │ ← Same as legacy_driver.driver_number (e.g., "12345")
└─────────────┘
```

## Mapping Example: End-to-End

**Scenario:** Generate report for Gurpal Singh

1. **Current System Lookup:**
   - User: Gurpal Singh
   - person.id = 123
   - driver.driver_number = "12345"

2. **Legacy System Mapping:**
   - legacy_driver.driver_number = "12345"
   - legacy_driver.id = 789
   - legacy_customer_charge.driver_id = 789 → Multiple charges

3. **Result:**
   - All charges with `driver_id = 789` are attributed to person_id 123 (Gurpal)
   - Report shows charges under Gurpal's name ✅

## Frontend Display

### Legacy Customer Management Page

**Endpoint:** `/api/legacy-customers/{customerId}/charges`

**Response:**
```json
[
  {
    "id": 456,
    "amount": 45.0,
    "date": "2026-05-10",
    "payment": 0.0,
    "driver": {
      "id": 789,
      "driverNumber": "12345",
      "name": "Gurpal Singh"
    },
    "customer": {
      "id": 100,
      "customerId": "CUST001",
      "name": "ABC Company"
    },
    "cab": null,
    "notes": "Airport run",
    "type": "CHARGE"
  }
]
```

**Frontend Display:**
```
Date: 2026-05-10
Amount: $45.00
Driver: 12345 (Gurpal Singh)
Customer: ABC Company
Type: CHARGE
Notes: Airport run
```

## Configuration

### Enable Legacy System for Tenant

```sql
UPDATE fareflow.tenant_config
SET use_legacy_charge_system = TRUE
WHERE tenant_id = 'fareflow_mac-cabs';
```

### Verify Configuration

```sql
-- Check which system is enabled
SELECT
    tenant_id,
    use_legacy_charge_system,
    CASE
        WHEN use_legacy_charge_system = TRUE THEN 'LEGACY'
        ELSE 'MODERN'
    END AS active_system
FROM fareflow.tenant_config;
```

## Logging

When legacy system is active, you'll see logs like:

```
[LEGACY CHARGE PROVIDER] Finding charges for driver person ID 123 from 2026-05-01 to 2026-05-31
[LEGACY] Mapping person ID 123 to driver_number 12345
[LEGACY] Found 15 legacy customer charges for driver_number 12345
```

## Troubleshooting

### Issue: No charges appear in driver report

**Check 1: Is legacy system enabled?**
```sql
SELECT use_legacy_charge_system FROM fareflow.tenant_config WHERE tenant_id = 'fareflow_YOUR-TENANT';
```

**Check 2: Does legacy_driver have mapping?**
```sql
SELECT * FROM legacy_driver WHERE driver_number = '12345';
```

**Check 3: Do charges exist in legacy table?**
```sql
SELECT COUNT(*)
FROM legacy_customer_charge lcc
JOIN legacy_driver ld ON lcc.driver_id = ld.id
WHERE ld.driver_number = '12345';
```

**Check 4: Does driver_number match between systems?**
```sql
-- Current system
SELECT d.driver_number, p.first_name, p.last_name
FROM driver d
JOIN person p ON d.person_id = p.id
WHERE p.id = 123;

-- Legacy system
SELECT driver_number, name
FROM legacy_driver
WHERE driver_number = '12345';
```

### Issue: Driver number mismatch

If driver numbers don't match, you need to update either:

```sql
-- Option 1: Fix legacy_driver.driver_number
UPDATE legacy_driver
SET driver_number = 'CORRECT-NUMBER'
WHERE id = 789;

-- Option 2: Fix current driver.driver_number
UPDATE driver
SET driver_number = 'LEGACY-NUMBER'
WHERE person_id = 123;
```

## Summary

✅ **person_id (current)** → **driver_number (stable)** → **legacy charges**

The system bridges legacy and modern data using `driver_number` as the stable business identifier, allowing seamless reporting regardless of which charge system is active.
