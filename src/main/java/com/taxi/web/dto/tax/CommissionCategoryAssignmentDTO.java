package com.taxi.web.dto.tax;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionCategoryAssignmentDTO {
    private Long id;
    private Long commissionTypeId;
    private String commissionTypeName;
    private Long revenueCategoryId;
    private String revenueCategoryName;
    private String revenueCategoryCode;
    private LocalDate assignedAt;
    private LocalDate unassignedAt;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
}
