package com.taxi.domain.charges.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Legacy driver mapping table.
 * Maps legacy driver IDs to driver_number (stable business key).
 * Used to attribute legacy charges to current drivers.
 * Driver names come from the current Driver table, not stored here.
 */
@Entity
@Table(name = "legacy_driver")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyDriver {

    @Id
    private Long id;  // Legacy driver ID (not auto-generated)

    @Column(name = "driver_number", nullable = false, unique = true, length = 50)
    private String driverNumber;
}
