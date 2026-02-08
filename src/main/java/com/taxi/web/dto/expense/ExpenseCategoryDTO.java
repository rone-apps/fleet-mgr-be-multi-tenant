package com.taxi.web.dto.expense;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ExpenseCategoryDTO - Data transfer object for expense categories
 *
 * Represents an expense category with its application type and target entity.
 * Returned when querying or creating expense categories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategoryDTO {

    private Long id;

    private String categoryCode;

    private String categoryName;

    private String description;

    private ExpenseCategory.CategoryType categoryType;  // FIXED or VARIABLE

    private ExpenseCategory.AppliesTo appliesTo;  // CAB, SHIFT, OWNER, DRIVER, COMPANY

    @JsonProperty("isActive")
    private boolean isActive;

    // Application Type System
    private ApplicationType applicationType;

    private String applicationTypeLabel;

    // Target entity references (only one is set based on applicationType)
    private Long shiftProfileId;
    private ShiftProfileRefDTO shiftProfile;

    private Long specificShiftId;
    private SimpleShiftDTO specificShift;

    private Long specificOwnerId;
    private SimpleOwnerDTO specificOwner;

    private Long specificDriverId;
    private SimpleDriverDTO specificDriver;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     */
    public static ExpenseCategoryDTO fromEntity(ExpenseCategory entity) {
        if (entity == null) {
            return null;
        }

        return ExpenseCategoryDTO.builder()
                .id(entity.getId())
                .categoryCode(entity.getCategoryCode())
                .categoryName(entity.getCategoryName())
                .description(entity.getDescription())
                .categoryType(entity.getCategoryType())
                .appliesTo(entity.getAppliesTo())
                .isActive(entity.isActive())
                .applicationType(entity.getApplicationType())
                .applicationTypeLabel(entity.getApplicationType() != null
                        ? entity.getApplicationType().getDisplayName()
                        : null)
                .shiftProfileId(entity.getShiftProfileId())
                .specificShiftId(entity.getSpecificShiftId())
                .specificOwnerId(entity.getSpecificOwnerId())
                .specificDriverId(entity.getSpecificDriverId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Simple DTO for shift references in expense category
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleShiftDTO {
        private Long id;
        private String cabNumber;
        private String shiftType;
        private String status;
    }

    /**
     * Simple DTO for owner references in expense category
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleOwnerDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String driverNumber;
    }

    /**
     * Simple DTO for driver references in expense category
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleDriverDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String driverNumber;
    }

    /**
     * Simple DTO for shift profile references
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShiftProfileRefDTO {
        private Long id;
        private String profileCode;
        private String profileName;
        private String colorCode;
    }
}
