package com.taxi.web.dto.cab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for shift configuration during cab creation
 * Allows per-shift attribute configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShiftRequest {

    private String shiftType; // "DAY" or "NIGHT"

    private Long ownerId; // Optional - Driver ID who will own this shift

    // Shift Profile - If not provided, default profile will be assigned
    private Long profileId; // Optional - ShiftProfile ID to assign to this shift

    // Shift attributes
    private String cabType; // SEDAN, HANDICAP_VAN, etc.
    private String shareType; // VOTING_SHARE, NON_VOTING_SHARE

    // Airport license
    private Boolean hasAirportLicense;
    private String airportLicenseNumber;
    private LocalDate airportLicenseExpiry;

    // Shift times (optional, defaults to standard times)
    private String startTime; // e.g., "06:00" for DAY, "18:00" for NIGHT
    private String endTime;   // e.g., "18:00" for DAY, "06:00" for NIGHT

    // Notes
    private String notes;
}
