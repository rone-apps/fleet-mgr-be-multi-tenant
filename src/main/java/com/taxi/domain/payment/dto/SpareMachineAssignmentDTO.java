package com.taxi.domain.payment.dto;

import com.taxi.domain.payment.model.SpareMachineAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpareMachineAssignmentDTO {
    private Long id;
    private Long spareMachineId;
    private String machineName;  // Convenience field for display
    private Integer realCabNumber;
    private String shift;  // "Day", "Night", or "BOTH"
    private LocalDateTime assignedAt;
    private LocalDateTime returnedAt;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;  // Computed for convenience

    public static SpareMachineAssignmentDTO fromEntity(SpareMachineAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return SpareMachineAssignmentDTO.builder()
                .id(assignment.getId())
                .spareMachineId(assignment.getSpareMachineId())
                .realCabNumber(assignment.getRealCabNumber())
                .shift(assignment.getShift())
                .assignedAt(assignment.getAssignedAt())
                .returnedAt(assignment.getReturnedAt())
                .notes(assignment.getNotes())
                .createdBy(assignment.getCreatedBy())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .isActive(assignment.isCurrentlyActive())
                .build();
    }
}
