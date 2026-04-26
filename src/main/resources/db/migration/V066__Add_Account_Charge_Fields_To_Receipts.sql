-- Add account charge specific fields to receipts table
-- These fields allow receipts to capture full trip information

ALTER TABLE receipts ADD COLUMN fare_amount DECIMAL(10, 2) NULL AFTER receipt_date;
ALTER TABLE receipts ADD COLUMN tip_amount DECIMAL(10, 2) NULL AFTER fare_amount;
ALTER TABLE receipts ADD COLUMN passenger_name VARCHAR(100) NULL AFTER tip_amount;
ALTER TABLE receipts ADD COLUMN start_time VARCHAR(8) NULL AFTER passenger_name;
ALTER TABLE receipts ADD COLUMN pickup_address VARCHAR(300) NULL AFTER start_time;
ALTER TABLE receipts ADD COLUMN dropoff_address VARCHAR(300) NULL AFTER pickup_address;

-- Create indexes for frequently filtered fields
CREATE INDEX idx_receipt_fare_amount ON receipts(fare_amount);
CREATE INDEX idx_receipt_passenger_name ON receipts(passenger_name);
CREATE INDEX idx_receipt_start_time ON receipts(start_time);
