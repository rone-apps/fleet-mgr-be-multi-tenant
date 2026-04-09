-- V058: Add shift column to spare_machine_assignment table
-- Tracks the shift (Day, Night, BOTH) for spare machine assignments

ALTER TABLE spare_machine_assignment
ADD COLUMN shift VARCHAR(10) DEFAULT 'BOTH' AFTER real_cab_number;
