USE fareflow;

-- Create expense_category_rule table
CREATE TABLE IF NOT EXISTS expense_category_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_category_id BIGINT NOT NULL,
    configuration_mode VARCHAR(20) NOT NULL,
    matching_criteria JSON,
    has_share_type_rule BOOLEAN DEFAULT FALSE,
    has_airport_license_rule BOOLEAN DEFAULT FALSE,
    has_cab_shift_type_rule BOOLEAN DEFAULT FALSE,
    has_cab_type_rule BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_category_rule (expense_category_id),
    KEY idx_ecr_mode (configuration_mode),
    KEY idx_ecr_share_rule (has_share_type_rule),
    KEY idx_ecr_airport_rule (has_airport_license_rule),
    KEY idx_ecr_shift_rule (has_cab_shift_type_rule),
    CONSTRAINT fk_expense_category FOREIGN KEY (expense_category_id)
        REFERENCES expense_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alter expense_category table to add new columns
ALTER TABLE expense_category ADD COLUMN IF NOT EXISTS supports_auto_matching BOOLEAN DEFAULT FALSE;
ALTER TABLE expense_category ADD COLUMN IF NOT EXISTS supports_individual_config BOOLEAN DEFAULT FALSE;

-- Add indexes on expense_category for new columns if they don't exist
ALTER TABLE expense_category ADD KEY idx_ec_auto_matching (supports_auto_matching);
ALTER TABLE expense_category ADD KEY idx_ec_individual_config (supports_individual_config);

-- Alter recurring_expense table to add auto-generation tracking
ALTER TABLE recurring_expense ADD COLUMN IF NOT EXISTS is_auto_generated BOOLEAN DEFAULT FALSE;
ALTER TABLE recurring_expense ADD COLUMN IF NOT EXISTS source_rule_id BIGINT;
ALTER TABLE recurring_expense ADD COLUMN IF NOT EXISTS generation_date TIMESTAMP NULL;

-- Add indexes on recurring_expense for new columns if they don't exist
ALTER TABLE recurring_expense ADD KEY idx_re_auto_generated (is_auto_generated);
ALTER TABLE recurring_expense ADD KEY idx_re_source_rule (source_rule_id);

-- Create recurring_expense_auto_creation table (audit trail)
CREATE TABLE IF NOT EXISTS recurring_expense_auto_creation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_category_rule_id BIGINT NOT NULL,
    recurring_expense_id BIGINT NOT NULL,
    creation_type VARCHAR(20) NOT NULL,
    matching_snapshot JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    KEY idx_reac_type (creation_type),
    KEY idx_reac_created (created_at),
    CONSTRAINT fk_category_rule FOREIGN KEY (expense_category_rule_id)
        REFERENCES expense_category_rule(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_expense FOREIGN KEY (recurring_expense_id)
        REFERENCES recurring_expense(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
