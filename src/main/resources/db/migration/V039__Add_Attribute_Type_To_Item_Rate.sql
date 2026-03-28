-- Add optional attribute_type_id to item_rate table
-- This allows per-trip rates (e.g., AIRPORT_TRIP) to be linked to a specific attribute
-- (e.g., TRANSPONDER → $7.00/trip, AIRPORT_PLATE → $6.50/trip)

ALTER TABLE item_rate
ADD COLUMN attribute_type_id BIGINT NULL;

ALTER TABLE item_rate
ADD CONSTRAINT fk_item_rate_attribute_type
FOREIGN KEY (attribute_type_id) REFERENCES cab_attribute_type(id);

CREATE INDEX idx_item_rate_attribute_type ON item_rate(attribute_type_id);
