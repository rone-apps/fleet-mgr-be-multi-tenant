-- Migration: Create Attribute Cost table for charging shifts based on attributes
-- Date: 2026-02-08
-- Purpose: Track costs for custom attributes assigned to shifts
--          Support historical pricing with effective date ranges

-- ============================================================================
-- Create Billing Unit Enum Type
-- ============================================================================

CREATE TABLE attribute_cost (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique identifier',

    -- Attribute Reference
    attribute_type_id BIGINT NOT NULL COMMENT 'FK to cab_attribute_type',

    -- Pricing Information
    price DECIMAL(10, 2) NOT NULL COMMENT 'Cost amount',
    billing_unit VARCHAR(20) NOT NULL COMMENT 'MONTHLY or DAILY',

    -- Temporal Information
    effective_from DATE NOT NULL COMMENT 'Start date for this cost',
    effective_to DATE DEFAULT NULL COMMENT 'End date (NULL = ongoing)',

    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Foreign Key Constraint
    CONSTRAINT fk_attribute_cost_attribute_type
        FOREIGN KEY (attribute_type_id)
        REFERENCES cab_attribute_type(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- Check Constraint: billing_unit must be valid
    CONSTRAINT chk_billing_unit_values
        CHECK (billing_unit IN ('MONTHLY', 'DAILY')),

    -- Check Constraint: effective_to must be after effective_from if set
    CONSTRAINT chk_effective_dates
        CHECK (effective_to IS NULL OR effective_to >= effective_from),

    -- Unique Constraint: Only one cost per attribute per date range
    CONSTRAINT uq_attribute_cost_date_range
        UNIQUE (attribute_type_id, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Pricing for custom attributes assigned to shifts with temporal tracking';

-- ============================================================================
-- Create Indexes for Performance
-- ============================================================================

CREATE INDEX idx_attribute_cost_attribute_type ON attribute_cost(attribute_type_id);
CREATE INDEX idx_attribute_cost_effective_from ON attribute_cost(effective_from);
CREATE INDEX idx_attribute_cost_effective_to ON attribute_cost(effective_to);
CREATE INDEX idx_attribute_cost_date_range ON attribute_cost(effective_from, effective_to);

-- ============================================================================
-- Notes
-- ============================================================================
--
-- Example Data:
-- INSERT INTO attribute_cost (attribute_type_id, price, billing_unit, effective_from, created_by)
-- VALUES (1, 30.00, 'MONTHLY', '2026-01-01', 'admin');
--
-- INSERT INTO attribute_cost (attribute_type_id, price, billing_unit, effective_from, effective_to, created_by)
-- VALUES (1, 35.00, 'MONTHLY', '2026-03-01', NULL, 'admin');
--
-- This means:
-- - From Jan 1 to Feb 28: Attribute 1 costs $30/month
-- - From Mar 1 onwards: Attribute 1 costs $35/month
--
-- When querying active costs on a date, use:
-- SELECT * FROM attribute_cost
-- WHERE attribute_type_id = ?
-- AND effective_from <= ?
-- AND (effective_to IS NULL OR effective_to >= ?)
--
