package com.taxi.web.dto.cab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating a new cab
 * Enhanced to include shift configuration during creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCabRequest {

    @Size(max = 20, message = "Cab number must not exceed 20 characters")
    private String cabNumber;  // User-provided cab number (must be numeric)

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @Size(max = 50, message = "Make must not exceed 50 characters")
    private String make;

    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    private Integer year;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    private String color;

    private String cabShiftType;  // SINGLE, DOUBLE - determines how many shifts to create

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // Date when cab was physically added to fleet
    private LocalDate fleetAddedDate;

    // ===== SHIFT CONFIGURATION =====
    // List of shifts to create with this cab
    // If empty/null, cab is created without shifts (requires manual shift creation later)
    @Valid
    private List<CreateShiftRequest> shifts;

    // ===== DEPRECATED FIELDS (for backward compatibility) =====
    // These are kept for backward compatibility but shifts[] is preferred
    @Deprecated
    // Validation removed - optional for backward compatibility
    private String cabType;  // SEDAN, HANDICAP_VAN - moved to shift level

    @Deprecated
    private String shareType;  // VOTING_SHARE, NON_VOTING_SHARE - moved to shift level

    @Deprecated
    private Boolean hasAirportLicense; // Moved to shift level

    @Deprecated
    private String airportLicenseNumber; // Moved to shift level

    @Deprecated
    private LocalDate airportLicenseExpiry; // Moved to shift level

    @Deprecated
    private Long ownerDriverId; // Moved to shift level (can be different per shift)
}
