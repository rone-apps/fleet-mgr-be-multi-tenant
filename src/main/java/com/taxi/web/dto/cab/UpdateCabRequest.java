package com.taxi.web.dto.cab;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for updating an existing cab
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCabRequest {

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @Size(max = 50, message = "Make must not exceed 50 characters")
    private String make;

    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    private Integer year;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    private String color;

    private String cabType;  // SEDAN, HANDICAP_VAN

    private String shareType;  // VOTING_SHARE, NON_VOTING_SHARE

    private String cabShiftType;  // SINGLE, DOUBLE

    private Boolean hasAirportLicense;

    @Size(max = 50, message = "Airport license number must not exceed 50 characters")
    private String airportLicenseNumber;

    private LocalDate airportLicenseExpiry;

    private String status;  // ACTIVE, MAINTENANCE, RETIRED

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // Optional owner driver ID (driver must be marked as owner)
    // Set to null to make cab company-owned
    private Long ownerDriverId;
}
