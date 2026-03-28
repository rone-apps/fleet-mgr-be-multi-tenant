-- Add ExpenseCategory records for dynamically-calculated expense types
-- so they can be linked to tax types via TaxCategoryAssignment.
-- These categories are NOT used for creating expenses directly — they exist
-- solely to enable tax configuration on these expense types.

INSERT INTO expense_category (category_code, category_name, description, category_type, applies_to, application_type, is_active, created_at, updated_at)
SELECT 'AIRPORT_TRIP', 'Airport Trip Expense', 'Per-trip airport charges (transponder or airport plate)', 'VARIABLE', 'DRIVER', 'ALL_ACTIVE_SHIFTS', 1, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM expense_category WHERE category_code = 'AIRPORT_TRIP');

INSERT INTO expense_category (category_code, category_name, description, category_type, applies_to, application_type, is_active, created_at, updated_at)
SELECT 'INSURANCE_MILEAGE', 'Insurance Mileage', 'Per-mile insurance charges based on mileage records', 'VARIABLE', 'DRIVER', 'ALL_ACTIVE_SHIFTS', 1, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM expense_category WHERE category_code = 'INSURANCE_MILEAGE');
