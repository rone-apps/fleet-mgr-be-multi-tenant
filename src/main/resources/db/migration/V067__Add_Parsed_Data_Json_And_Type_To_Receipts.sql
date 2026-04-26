ALTER TABLE receipts
  ADD COLUMN parsed_data_json LONGTEXT NULL,
  ADD COLUMN receipt_type VARCHAR(50) NULL;

CREATE INDEX idx_receipt_type ON receipts (receipt_type);
