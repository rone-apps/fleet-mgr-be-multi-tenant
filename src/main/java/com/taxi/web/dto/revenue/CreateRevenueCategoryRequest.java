package com.taxi.web.dto.revenue;

import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.revenue.entity.RevenueCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new revenue category
 * Validates that application type and related fields are consistent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRevenueCategoryRequest {

    @NotBlank(message = "Category code is required")
    @Size(min = 3, max = 50, message = "Category code must be between 3 and 50 characters")
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    @Size(min = 3, max = 100, message = "Category name must be between 3 and 100 characters")
    private String categoryName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Applies to is required")
    private RevenueCategory.AppliesTo appliesTo;

    @NotNull(message = "Category type is required")
    private RevenueCategory.CategoryType categoryType;

    @NotNull(message = "Application type is required")
    private ApplicationType applicationType;

    private Long shiftProfileId;

    private Long specificShiftId;

    private Long specificOwnerId;

    private Long specificDriverId;

    @Builder.Default
    private Boolean isActive = true;

    /**
     * Validate that application type and related fields are consistent
     */
    @AssertTrue(message = "Application type and related fields must be consistent")
    public boolean isApplicationTypeValid() {
        if (applicationType == null) {
            return false;
        }

        switch (applicationType) {
            case SHIFT_PROFILE:
                return shiftProfileId != null;
            case SPECIFIC_SHIFT:
                return specificShiftId != null;
            case SPECIFIC_OWNER_DRIVER:
                // XOR: Either owner or driver, but not both or neither
                boolean hasOwner = specificOwnerId != null;
                boolean hasDriver = specificDriverId != null;
                return hasOwner != hasDriver;
            case ALL_ACTIVE_SHIFTS:
            case ALL_NON_OWNER_DRIVERS:
                return true;
            default:
                return false;
        }
    }
}
