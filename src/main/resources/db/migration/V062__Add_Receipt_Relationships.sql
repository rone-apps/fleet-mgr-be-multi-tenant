-- Add foreign key columns to receipts table for cab, shift, and owner
ALTER TABLE receipts ADD COLUMN cab_id BIGINT;
ALTER TABLE receipts ADD COLUMN shift_id BIGINT;
ALTER TABLE receipts ADD COLUMN owner_id BIGINT;

-- Add foreign key constraints
ALTER TABLE receipts
ADD CONSTRAINT fk_receipt_cab
FOREIGN KEY (cab_id) REFERENCES cab(id) ON DELETE SET NULL;

ALTER TABLE receipts
ADD CONSTRAINT fk_receipt_shift
FOREIGN KEY (shift_id) REFERENCES driver_shifts(id) ON DELETE SET NULL;

ALTER TABLE receipts
ADD CONSTRAINT fk_receipt_owner
FOREIGN KEY (owner_id) REFERENCES driver(id) ON DELETE SET NULL;

-- Add indexes for foreign keys
CREATE INDEX idx_receipt_cab_id ON receipts(cab_id);
CREATE INDEX idx_receipt_shift_id ON receipts(shift_id);
CREATE INDEX idx_receipt_owner_id ON receipts(owner_id);
