-- Add status column to cab table
ALTER TABLE cab ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Add index for status filtering
CREATE INDEX idx_cab_status ON cab (status);
