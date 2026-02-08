package com.taxi.web.dto.profile;

import com.taxi.domain.profile.model.ShiftProfileAssignment;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ShiftProfileAssignmentDTO - DTO for profile assignment history records
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class ShiftProfileAssignmentDTO {

    private Long id;
    private Long shiftId;
    private Long profileId;
    private String profileName;
    private String profileCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String assignedBy;
    private LocalDateTime createdAt;
    private Boolean isActive;  // end_date IS NULL

    /**
     * Convert entity to DTO
     */
    public static ShiftProfileAssignmentDTO fromEntity(ShiftProfileAssignment entity) {
        if (entity == null) {
            return null;
        }

        return ShiftProfileAssignmentDTO.builder()
                .id(entity.getId())
                .shiftId(entity.getShift() != null ? entity.getShift().getId() : null)
                .profileId(entity.getProfile() != null ? entity.getProfile().getId() : null)
                .profileName(entity.getProfile() != null ? entity.getProfile().getProfileName() : null)
                .profileCode(entity.getProfile() != null ? entity.getProfile().getProfileCode() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .assignedBy(entity.getAssignedBy())
                .createdAt(entity.getCreatedAt())
                .isActive(entity.isActive())
                .build();
    }
}
