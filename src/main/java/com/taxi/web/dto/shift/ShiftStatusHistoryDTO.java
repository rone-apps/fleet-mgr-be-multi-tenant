package com.taxi.web.dto.shift;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.shift.model.ShiftStatusHistory;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for ShiftStatusHistory responses
 *
 * Represents a status history record for a shift.
 * Provides the audit trail for when a shift became active or inactive.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftStatusHistoryDTO {

    private Long id;

    private Long shiftId;

    @JsonProperty("isActive")
    private Boolean isActive;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String reason;

    private String changedBy;

    private LocalDateTime createdAt;

    /**
     * Convenience properties for frontend
     */
    @JsonProperty("isCurrent")
    private Boolean isCurrent;  // true if effectiveTo is NULL

    @JsonProperty("statusLabel")
    private String statusLabel;  // "Active" or "Inactive"

    /**
     * Convert ShiftStatusHistory entity to DTO
     */
    public static ShiftStatusHistoryDTO fromEntity(ShiftStatusHistory history) {
        if (history == null) {
            return null;
        }

        Boolean isCurrent = history.getEffectiveTo() == null;
        String statusLabel = Boolean.TRUE.equals(history.getIsActive()) ? "Active" : "Inactive";

        return ShiftStatusHistoryDTO.builder()
                .id(history.getId())
                .shiftId(history.getShift() != null ? history.getShift().getId() : null)
                .isActive(history.getIsActive())
                .effectiveFrom(history.getEffectiveFrom())
                .effectiveTo(history.getEffectiveTo())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .isCurrent(isCurrent)
                .statusLabel(statusLabel)
                .build();
    }
}
