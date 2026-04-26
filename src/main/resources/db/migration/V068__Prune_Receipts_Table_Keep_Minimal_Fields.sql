-- Prune receipts table: keep only minimal fields
-- All detailed data is in parsed_data_json (raw JSON from AI)

ALTER TABLE receipts
DROP COLUMN IF EXISTS fare_amount,
DROP COLUMN IF EXISTS tip_amount,
DROP COLUMN IF EXISTS passenger_name,
DROP COLUMN IF EXISTS pickup_address,
DROP COLUMN IF EXISTS dropoff_address,
DROP COLUMN IF EXISTS start_time,
DROP COLUMN IF EXISTS document_type,
DROP COLUMN IF EXISTS vendor_name,
DROP COLUMN IF EXISTS total_amount,
DROP COLUMN IF EXISTS tax_amount,
DROP COLUMN IF EXISTS receipt_date,
DROP COLUMN IF EXISTS line_items_json,
DROP COLUMN IF EXISTS raw_claude_response,
DROP COLUMN IF EXISTS notes,
DROP COLUMN IF EXISTS shift_id,
DROP COLUMN IF EXISTS account_customer_id,
DROP COLUMN IF EXISTS shift_type;

-- Keep only:
-- id, receipt_type, cab_id, owner_id, parsed_data_json, image_data, image_mime_type, status, created_at, updated_at
