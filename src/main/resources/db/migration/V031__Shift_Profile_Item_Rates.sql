-- V031: Per-Unit Item Rates System
-- Adds global per-unit expense rates (mileage, airport trips, etc.) with override capability

-- Base/default per-unit expense rates (global system rates)
CREATE TABLE item_rate (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    unit_type        VARCHAR(30)  NOT NULL CHECK (unit_type IN ('MILEAGE','AIRPORT_TRIP')),
    rate             DECIMAL(10,4) NOT NULL,
    charged_to       VARCHAR(10)  NOT NULL CHECK (charged_to IN ('DRIVER','OWNER')),
    effective_from   DATE NOT NULL,
    effective_to     DATE,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Per-owner or per-shift item rate overrides (like lease rate overrides)
CREATE TABLE item_rate_override (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_rate_id          BIGINT NOT NULL REFERENCES item_rate(id) ON DELETE CASCADE,
    owner_driver_number   VARCHAR(50) NOT NULL,
    cab_number            VARCHAR(50),
    shift_type            VARCHAR(20),
    day_of_week           VARCHAR(20),
    override_rate         DECIMAL(10,4) NOT NULL,
    priority              INTEGER NOT NULL DEFAULT 10,
    start_date            DATE NOT NULL,
    end_date              DATE,
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    notes                 VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(50),
    updated_by            VARCHAR(50)
);

-- Indexes for efficient querying
CREATE INDEX idx_item_rate_active ON item_rate(is_active);
CREATE INDEX idx_item_rate_unit_type ON item_rate(unit_type);
CREATE INDEX idx_item_rate_effective_date ON item_rate(effective_from, effective_to);
CREATE INDEX idx_item_rate_override_item ON item_rate_override(item_rate_id);
CREATE INDEX idx_item_rate_override_owner_cab ON item_rate_override(owner_driver_number, cab_number);
CREATE INDEX idx_item_rate_override_dates ON item_rate_override(start_date, end_date);
CREATE INDEX idx_item_rate_override_active ON item_rate_override(is_active);

-- Add airport trip count to shift_log
ALTER TABLE shift_log ADD COLUMN airport_trip_count INTEGER NOT NULL DEFAULT 0;
