-- Add shift column to merchant2cab table
-- Values: BOTH (shared machine), DAY, NIGHT (separate merchant per shift)
ALTER TABLE merchant2cab ADD COLUMN shift VARCHAR(10) NOT NULL DEFAULT 'BOTH';

-- Drop old unique constraint and recreate with shift included
ALTER TABLE merchant2cab DROP INDEX IF EXISTS uk_cab_merchant_active;
ALTER TABLE merchant2cab ADD CONSTRAINT uk_cab_merchant_active
    UNIQUE (cab_number, merchant_number, start_date, shift);

-- Add index for cab+shift lookups
CREATE INDEX idx_cab_shift ON merchant2cab (cab_number, shift);
