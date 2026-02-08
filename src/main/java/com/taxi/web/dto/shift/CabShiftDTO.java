package com.taxi.web.dto.shift;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.shift.model.CabShift;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for CabShift responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabShiftDTO {

    private Long id;
    
    // Cab information
    private Long cabId;
    private String cabNumber;
    private String cabRegistration;
    
    // Shift information
    private String shiftType;  // DAY, NIGHT
    private String shiftTypeDisplay;  // "Day Shift", "Night Shift"
    
    // Editable shift times (per cab)
    private String startTime;  // e.g., "06:00"
    private String endTime;    // e.g., "18:00"
    private String shiftHours;  // Formatted: "06:00 - 18:00"
    
    // Current owner information
    private Long currentOwnerId;
    private String currentOwnerDriverNumber;
    private String currentOwnerName;
    
    // Status
    private String status;  // ACTIVE, INACTIVE

    @JsonProperty("isActive")
    private boolean isActive;

    // Shift-level attributes
    private String cabType;  // SEDAN, HANDICAP_VAN
    private String shareType;  // VOTING_SHARE, NON_VOTING_SHARE, null
    private Boolean hasAirportLicense;
    private String airportLicenseNumber;
    private LocalDate airportLicenseExpiry;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Profile information
    private CurrentProfileDTO currentProfile;

    /**
     * DTO for current profile (nested)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentProfileDTO {
        private Long id;
        private String profileCode;
        private String profileName;
        private String category;
        private String colorCode;
    }

    /**
     * Convert CabShift entity to DTO
     */
    public static CabShiftDTO fromEntity(CabShift shift) {
        if (shift == null) {
            return null;
        }

        CabShiftDTOBuilder builder = CabShiftDTO.builder()
                .id(shift.getId())
                .shiftType(shift.getShiftType().name())
                .shiftTypeDisplay(shift.getShiftType().getDisplayName())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .shiftHours(shift.getStartTime() + " - " + shift.getEndTime())
                .status(shift.getStatus().name())
                .isActive(shift.getStatus() == CabShift.ShiftStatus.ACTIVE)
                .cabType(shift.getCabType() != null ? shift.getCabType().name() : null)
                .shareType(shift.getShareType() != null ? shift.getShareType().name() : null)
                .hasAirportLicense(shift.getHasAirportLicense())
                .airportLicenseNumber(shift.getAirportLicenseNumber())
                .airportLicenseExpiry(shift.getAirportLicenseExpiry())
                .notes(shift.getNotes())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt());

        // Add cab info
        if (shift.getCab() != null) {
            builder.cabId(shift.getCab().getId())
                   .cabNumber(shift.getCab().getCabNumber())
                   .cabRegistration(shift.getCab().getRegistrationNumber());
        }

        // Add current owner info
        if (shift.getCurrentOwner() != null) {
            builder.currentOwnerId(shift.getCurrentOwner().getId())
                   .currentOwnerDriverNumber(shift.getCurrentOwner().getDriverNumber())
                   .currentOwnerName(shift.getCurrentOwner().getFirstName() + " " + shift.getCurrentOwner().getLastName());
        }

        // Add current profile info
        if (shift.getCurrentProfile() != null) {
            builder.currentProfile(CurrentProfileDTO.builder()
                    .id(shift.getCurrentProfile().getId())
                    .profileCode(shift.getCurrentProfile().getProfileCode())
                    .profileName(shift.getCurrentProfile().getProfileName())
                    .category(shift.getCurrentProfile().getCategory())
                    .colorCode(shift.getCurrentProfile().getColorCode())
                    .build());
        }

        return builder.build();
    }
}
