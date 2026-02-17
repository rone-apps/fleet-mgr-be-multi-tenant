package com.taxi.web.dto.revenue;

import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.revenue.entity.RevenueCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for RevenueCategory with application type information
 * Used for displaying revenue categories with their application targets
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueCategoryDTO {

    private Long id;

    private String categoryCode;

    private String categoryName;

    private String description;

    private RevenueCategory.AppliesTo appliesTo;

    private RevenueCategory.CategoryType categoryType;

    private Boolean isActive;

    private ApplicationType applicationType;

    private Long shiftProfileId;

    private Long specificShiftId;

    private Long specificPersonId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     */
    public static RevenueCategoryDTO fromEntity(RevenueCategory entity) {
        if (entity == null) {
            return null;
        }

        return RevenueCategoryDTO.builder()
                .id(entity.getId())
                .categoryCode(entity.getCategoryCode())
                .categoryName(entity.getCategoryName())
                .description(entity.getDescription())
                .appliesTo(entity.getAppliesTo())
                .categoryType(entity.getCategoryType())
                .isActive(entity.getIsActive())
                .applicationType(entity.getApplicationType())
                .shiftProfileId(entity.getShiftProfileId())
                .specificShiftId(entity.getSpecificShiftId())
                .specificPersonId(entity.getSpecificPersonId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
