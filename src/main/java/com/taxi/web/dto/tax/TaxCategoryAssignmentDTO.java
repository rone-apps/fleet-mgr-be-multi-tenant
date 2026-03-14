package com.taxi.web.dto.tax;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCategoryAssignmentDTO {
    private Long id;
    private Long taxTypeId;
    private String taxTypeName;
    private Long expenseCategoryId;
    private String expenseCategoryName;
    private String expenseCategoryCode;
    private LocalDate assignedAt;
    private LocalDate unassignedAt;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
}
