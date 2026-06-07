package com.taxi.web.dto.cab;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO for per-shift configuration during cab reactivation with new owners
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactivateShiftRequest {
    private Long shiftId;  // The shift being reactivated
    private Long ownerId;  // New owner (or null if keeping same)
    private Long profileId;  // New profile (or null to auto-assign default)
    private String cabType;  // SEDAN, HANDICAP_VAN
    private String shareType;  // VOTING_SHARE, NON_VOTING_SHARE
    private Boolean hasAirportLicense;
    private String airportLicenseNumber;
    private LocalDate airportLicenseExpiry;
    private String notes;
}
