package com.taxi.web.dto.cab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.shift.model.CabShift;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Cab responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabDTO {

    private Long id;
    private String cabNumber;
    private String registrationNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private String cabType;
    private String shareType;
    private String cabShiftType;

    @JsonProperty("hasAirportLicense")
    private boolean hasAirportLicense;

    private String airportLicenseNumber;
    private LocalDate airportLicenseExpiry;

    @JsonProperty("airportLicenseExpired")
    private boolean airportLicenseExpired;

    private String status;
    private String notes;

    // Shift count (1 or 2 active shifts)
    private int shiftCount;

    // Owner information
    private Long ownerDriverId;
    private String ownerDriverNumber;
    private String ownerDriverName;

    @JsonProperty("isCompanyOwned")
    private boolean isCompanyOwned;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Cab entity to DTO
     * Derives shift-level attributes (cabType, shareType, airport license) from the cab's shifts
     */
    public static CabDTO fromEntity(Cab cab) {
        if (cab == null) {
            return null;
        }

        CabDTOBuilder builder = CabDTO.builder()
                .id(cab.getId())
                .cabNumber(cab.getCabNumber())
                .registrationNumber(cab.getRegistrationNumber())
                .make(cab.getMake())
                .model(cab.getModel())
                .year(cab.getYear())
                .color(cab.getColor())
                .status(cab.getStatus() != null ? cab.getStatus().name() : "ACTIVE")
                .notes(cab.getNotes())
                .isCompanyOwned(cab.isCompanyOwned())
                .createdAt(cab.getCreatedAt())
                .updatedAt(cab.getUpdatedAt());

        // Derive attributes from shifts
        try {
            List<CabShift> shifts = cab.getShifts();
            if (shifts != null && !shifts.isEmpty()) {
                int activeShifts = 0;
                boolean anyAirport = false;
                boolean anyExpired = false;
                String airportLicNum = null;
                LocalDate airportLicExp = null;
                String derivedCabType = null;
                String derivedShareType = null;

                for (CabShift shift : shifts) {
                    if (shift.getStatus() == CabShift.ShiftStatus.ACTIVE) {
                        activeShifts++;
                    }
                    if (Boolean.TRUE.equals(shift.getHasAirportLicense())) {
                        anyAirport = true;
                        if (shift.getAirportLicenseNumber() != null) {
                            airportLicNum = shift.getAirportLicenseNumber();
                        }
                        if (shift.getAirportLicenseExpiry() != null) {
                            airportLicExp = shift.getAirportLicenseExpiry();
                        }
                        if (shift.isAirportLicenseExpired()) {
                            anyExpired = true;
                        }
                    }
                    if (shift.getCabType() != null && derivedCabType == null) {
                        derivedCabType = shift.getCabType().name();
                    }
                    if (shift.getShareType() != null && derivedShareType == null) {
                        derivedShareType = shift.getShareType().name();
                    }
                }

                builder.shiftCount(activeShifts)
                       .cabShiftType(activeShifts >= 2 ? "DOUBLE" : activeShifts == 1 ? "SINGLE" : null)
                       .hasAirportLicense(anyAirport)
                       .airportLicenseNumber(airportLicNum)
                       .airportLicenseExpiry(airportLicExp)
                       .airportLicenseExpired(anyExpired)
                       .cabType(derivedCabType)
                       .shareType(derivedShareType);
            } else {
                builder.shiftCount(0)
                       .cabShiftType(null)
                       .hasAirportLicense(false)
                       .cabType(null)
                       .shareType(null);
            }
        } catch (Exception e) {
            // Lazy loading might fail if called outside transaction
            builder.shiftCount(0)
                   .cabShiftType(null)
                   .hasAirportLicense(false)
                   .cabType(null)
                   .shareType(null);
        }

        // Add owner information if present
        if (cab.getOwnerDriver() != null) {
            builder.ownerDriverId(cab.getOwnerDriver().getId())
                   .ownerDriverNumber(cab.getOwnerDriver().getDriverNumber())
                   .ownerDriverName(cab.getOwnerDriver().getFirstName() + " " + cab.getOwnerDriver().getLastName());
        }

        return builder.build();
    }
}
