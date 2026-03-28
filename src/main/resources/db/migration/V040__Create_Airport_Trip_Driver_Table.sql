-- ============================================================
-- V040: Create airport_trip_driver table
-- Stores per-driver breakdown of hourly airport trips.
-- Populated during CSV upload by matching hours to driver_shifts.
-- ============================================================

CREATE TABLE IF NOT EXISTS airport_trip_driver (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    airport_trip_id BIGINT       NOT NULL,
    cab_number      VARCHAR(50)  NOT NULL,
    driver_number   VARCHAR(50)  NOT NULL,
    trip_date       DATE         NOT NULL,
    hour            INT          NOT NULL,
    trip_count      INT          NOT NULL DEFAULT 0,
    total_daily_trips INT        NULL,
    assignment_method VARCHAR(20) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_atd_airport_trip FOREIGN KEY (airport_trip_id) REFERENCES airport_trips(id) ON DELETE CASCADE,

    INDEX idx_atd_airport_trip (airport_trip_id),
    INDEX idx_atd_driver_number (driver_number),
    INDEX idx_atd_trip_date (trip_date),
    INDEX idx_atd_driver_date (driver_number, trip_date)
);
