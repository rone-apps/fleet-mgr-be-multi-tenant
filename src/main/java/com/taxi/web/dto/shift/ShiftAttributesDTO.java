package com.taxi.web.dto.shift;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.shift.model.CabShift;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for Shift Attributes
 *
 * Represents all attributes associated with a shift.
 * Attributes are now managed at the shift level, allowing different values
 * for DAY and NIGHT shifts of the same cab.
 *
 * This DTO is used for both GET (retrieve) and PUT (update) operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAttributesDTO {

    /**
     * Cab type: SEDAN, HANDICAP_VAN
     */
    private CabType cabType;

    /**
     * Share type: VOTING_SHARE, NON_VOTING_SHARE, or null
     */
    private ShareType shareType;

    /**
     * Whether this shift has an airport license
     */
    private Boolean hasAirportLicense;

    /**
     * Airport license number (only relevant if hasAirportLicense is true)
     */
    private String airportLicenseNumber;

    /**
     * Airport license expiry date
     */
    private LocalDate airportLicenseExpiry;

    /**
     * Convenience properties for frontend
     */
    private Boolean isAirportLicenseExpired;

    /**
     * Convert CabShift entity to attributes DTO
     */
    public static ShiftAttributesDTO fromEntity(CabShift shift) {
        if (shift == null) {
            return null;
        }

        Boolean isExpired = shift.isAirportLicenseExpired();

        return ShiftAttributesDTO.builder()
                .cabType(shift.getCabType())
                .shareType(shift.getShareType())
                .hasAirportLicense(shift.getHasAirportLicense())
                .airportLicenseNumber(shift.getAirportLicenseNumber())
                .airportLicenseExpiry(shift.getAirportLicenseExpiry())
                .isAirportLicenseExpired(isExpired)
                .build();
    }
}
