package com.taxi.web.dto.cab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.cab.model.Cab;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
                .cabType(cab.getCabType() != null ? cab.getCabType().name() : null)
                .shareType(cab.getShareType() != null ? cab.getShareType().name() : null)
                .cabShiftType(cab.getCabShiftType() != null ? cab.getCabShiftType().name() : null)
                .hasAirportLicense(Boolean.TRUE.equals(cab.getHasAirportLicense()))
                .airportLicenseNumber(cab.getAirportLicenseNumber())
                .airportLicenseExpiry(cab.getAirportLicenseExpiry())
                .airportLicenseExpired(cab.isAirportLicenseExpired())
                .status(cab.getStatus() != null ? cab.getStatus().name() : null)
                .notes(cab.getNotes())
                .isCompanyOwned(cab.isCompanyOwned())
                .createdAt(cab.getCreatedAt())
                .updatedAt(cab.getUpdatedAt());

        // Add owner information if present
        if (cab.getOwnerDriver() != null) {
            builder.ownerDriverId(cab.getOwnerDriver().getId())
                   .ownerDriverNumber(cab.getOwnerDriver().getDriverNumber())
                   .ownerDriverName(cab.getOwnerDriver().getFirstName() + " " + cab.getOwnerDriver().getLastName());
        }

        return builder.build();
    }
}
