package com.taxi.web.dto.cab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.cab.model.CabOwnerHistory;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for Cab Owner History responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabOwnerHistoryDTO {

    private Long id;
    private Long cabId;
    private String cabNumber;
    private Long ownerDriverId;
    private String ownerDriverNumber;
    private String ownerDriverName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @JsonProperty("isCurrent")
    private boolean isCurrent;
    
    private String notes;

    /**
     * Convert entity to DTO
     */
    public static CabOwnerHistoryDTO fromEntity(CabOwnerHistory history) {
        if (history == null) {
            return null;
        }

        return CabOwnerHistoryDTO.builder()
                .id(history.getId())
                .cabId(history.getCab().getId())
                .cabNumber(history.getCab().getCabNumber())
                .ownerDriverId(history.getOwnerDriver().getId())
                .ownerDriverNumber(history.getOwnerDriver().getDriverNumber())
                .ownerDriverName(history.getOwnerDriver().getFirstName() + " " + history.getOwnerDriver().getLastName())
                .startDate(history.getStartDate())
                .endDate(history.getEndDate())
                .isCurrent(history.isCurrent())
                .notes(history.getNotes())
                .build();
    }
}
