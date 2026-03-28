-- Link existing AIRPORT_TRIP item rates to their corresponding cab attribute types
-- Transponder Airport Trip Fee → TRANSPONDER attribute type ($7.00/trip)
-- Airport Plate Trip Fee → AIRPORT_PLATE attribute type ($6.50/trip)

UPDATE item_rate
SET attribute_type_id = (SELECT id FROM cab_attribute_type WHERE attribute_code = 'TRANSPONDER' LIMIT 1)
WHERE name = 'Transponder Airport Trip Fee'
  AND unit_type = 'AIRPORT_TRIP'
  AND attribute_type_id IS NULL;

UPDATE item_rate
SET attribute_type_id = (SELECT id FROM cab_attribute_type WHERE attribute_code = 'AIRPORT_PLATE' LIMIT 1)
WHERE name = 'Airport Plate Trip Fee'
  AND unit_type = 'AIRPORT_TRIP'
  AND attribute_type_id IS NULL;
