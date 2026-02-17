-- Add ApplicationType support to other_revenue table
-- Mirrors the OneTimeExpense model to support targeted revenue allocations

ALTER TABLE other_revenue
ADD COLUMN application_type VARCHAR(30) DEFAULT NULL,
ADD COLUMN shift_profile_id BIGINT DEFAULT NULL,
ADD COLUMN specific_shift_id BIGINT DEFAULT NULL,
ADD COLUMN specific_person_id BIGINT DEFAULT NULL,
ADD COLUMN attribute_type_id BIGINT DEFAULT NULL;

-- Create indexes for new columns
CREATE INDEX idx_other_revenue_app_type ON other_revenue(application_type);
CREATE INDEX idx_other_revenue_shift_profile ON other_revenue(shift_profile_id);
CREATE INDEX idx_other_revenue_specific_shift ON other_revenue(specific_shift_id);
CREATE INDEX idx_other_revenue_specific_person ON other_revenue(specific_person_id);
CREATE INDEX idx_other_revenue_attribute_type ON other_revenue(attribute_type_id);

-- Combined index for filtering by application type and date range
CREATE INDEX idx_other_revenue_app_type_date ON other_revenue(application_type, revenue_date);

-- Foreign key constraints for new fields
ALTER TABLE other_revenue
ADD CONSTRAINT fk_other_revenue_shift_profile
  FOREIGN KEY (shift_profile_id) REFERENCES shift_profile(id),
ADD CONSTRAINT fk_other_revenue_specific_shift
  FOREIGN KEY (specific_shift_id) REFERENCES cab_shift(id),
ADD CONSTRAINT fk_other_revenue_specific_person
  FOREIGN KEY (specific_person_id) REFERENCES driver(id);
