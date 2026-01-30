-- Create cab_attribute_type table
-- Master list of attribute definitions that can be assigned to cabs
-- MySQL 5.7+ compatible version
CREATE TABLE IF NOT EXISTS cab_attribute_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique attribute type ID',
    attribute_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Unique code identifier (e.g., AIRPORT_LICENSE)',
    attribute_name VARCHAR(100) NOT NULL COMMENT 'Display name (e.g., Airport License)',
    description VARCHAR(500) COMMENT 'Description of the attribute',
    category VARCHAR(20) NOT NULL COMMENT 'Category: LICENSE, EQUIPMENT, TYPE, PERMIT, CERTIFICATION',
    data_type VARCHAR(20) NOT NULL COMMENT 'Data type: STRING, NUMBER, DATE, BOOLEAN',
    requires_value TINYINT(1) DEFAULT 0 COMMENT 'Whether this attribute requires a value',
    validation_pattern VARCHAR(255) COMMENT 'Optional regex pattern for validation',
    help_text VARCHAR(500) COMMENT 'Help text for users',
    is_active TINYINT(1) DEFAULT 1 COMMENT 'Whether this attribute type is active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    INDEX idx_attr_type_active (is_active),
    INDEX idx_attr_type_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Master list of attribute types that can be assigned to cabs';

-- Create cab_attribute_value table
-- Temporal tracking of cab attributes with full history support
-- NULL end_date means the attribute is currently active
CREATE TABLE IF NOT EXISTS cab_attribute_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique attribute assignment ID',
    cab_id BIGINT NOT NULL COMMENT 'Reference to cab',
    attribute_type_id BIGINT NOT NULL COMMENT 'Reference to attribute type',
    attribute_value VARCHAR(255) COMMENT 'Optional value (e.g., license number)',
    start_date DATE NOT NULL COMMENT 'Start date of assignment',
    end_date DATE COMMENT 'End date of assignment (NULL = currently active)',
    notes VARCHAR(500) COMMENT 'Optional notes about this assignment',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(50) COMMENT 'User who created this assignment',
    updated_by VARCHAR(50) COMMENT 'User who last updated this assignment',
    CONSTRAINT fk_cab_attr_val_cab FOREIGN KEY (cab_id) REFERENCES cab(id) ON DELETE CASCADE,
    CONSTRAINT fk_cab_attr_val_type FOREIGN KEY (attribute_type_id) REFERENCES cab_attribute_type(id),
    INDEX idx_attr_val_cab (cab_id),
    INDEX idx_attr_val_type (attribute_type_id),
    INDEX idx_attr_val_dates (start_date, end_date),
    INDEX idx_attr_val_cab_type (cab_id, attribute_type_id),
    INDEX idx_attr_val_current (cab_id, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Temporal tracking of cab attributes with full history';

-- Insert predefined attribute types (ignore duplicates)
INSERT IGNORE INTO cab_attribute_type (
    attribute_code,
    attribute_name,
    description,
    category,
    data_type,
    requires_value,
    is_active
) VALUES
    ('AIRPORT_LICENSE', 'Airport License', 'License to operate at airports', 'LICENSE', 'STRING', 1, 1),
    ('TRANSPONDER', 'Transponder', 'Electronic transponder for tracking', 'EQUIPMENT', 'STRING', 1, 1),
    ('VEHICLE_TYPE_VAN', 'Van Classification', 'Vehicle classified as van', 'TYPE', 'BOOLEAN', 0, 1),
    ('VEHICLE_TYPE_SEDAN', 'Sedan Classification', 'Vehicle classified as sedan', 'TYPE', 'BOOLEAN', 0, 1),
    ('HANDICAP_ACCESSIBLE', 'Handicap Accessible', 'Vehicle is handicap accessible', 'TYPE', 'BOOLEAN', 0, 1);
