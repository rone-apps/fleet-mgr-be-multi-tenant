package com.taxi.web.dto.shift;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.shift.model.ShiftOwnership;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for ShiftOwnership responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftOwnershipDTO {

    private Long id;
    
    // Shift information
    private Long shiftId;
    private Long cabId;
    private String cabNumber;
    private String shiftType;
    private String shiftTypeDisplay;
    
    // Owner information
    private Long ownerId;
    private String ownerDriverNumber;
    private String ownerName;
    
    // Ownership period
    private LocalDate startDate;
    private LocalDate endDate;
    
    @JsonProperty("isCurrent")
    private boolean isCurrent;
    
    // Financial information
    private String acquisitionType;
    private BigDecimal acquisitionPrice;
    private BigDecimal salePrice;
    
    // Transfer information
    private Long transferredToId;
    private String transferredToDriverNumber;
    private String transferredToName;
    
    private String notes;

    /**
     * Convert ShiftOwnership entity to DTO
     */
    public static ShiftOwnershipDTO fromEntity(ShiftOwnership ownership) {
        if (ownership == null) {
            return null;
        }

        ShiftOwnershipDTOBuilder builder = ShiftOwnershipDTO.builder()
                .id(ownership.getId())
                .startDate(ownership.getStartDate())
                .endDate(ownership.getEndDate())
                .isCurrent(ownership.isCurrent())
                .acquisitionType(ownership.getAcquisitionType() != null ? ownership.getAcquisitionType().name() : null)
                .acquisitionPrice(ownership.getAcquisitionPrice())
                .salePrice(ownership.getSalePrice())
                .notes(ownership.getNotes());

        // Add shift info
        if (ownership.getShift() != null) {
            builder.shiftId(ownership.getShift().getId())
                   .shiftType(ownership.getShift().getShiftType().name())
                   .shiftTypeDisplay(ownership.getShift().getShiftType().getDisplayName());
            
            if (ownership.getShift().getCab() != null) {
                builder.cabId(ownership.getShift().getCab().getId())
                       .cabNumber(ownership.getShift().getCab().getCabNumber());
            }
        }

        // Add owner info
        if (ownership.getOwner() != null) {
            builder.ownerId(ownership.getOwner().getId())
                   .ownerDriverNumber(ownership.getOwner().getDriverNumber())
                   .ownerName(ownership.getOwner().getFirstName() + " " + ownership.getOwner().getLastName());
        }

        // Add transferred to info
        if (ownership.getTransferredTo() != null) {
            builder.transferredToId(ownership.getTransferredTo().getId())
                   .transferredToDriverNumber(ownership.getTransferredTo().getDriverNumber())
                   .transferredToName(ownership.getTransferredTo().getFirstName() + " " + ownership.getTransferredTo().getLastName());
        }

        return builder.build();
    }
}
