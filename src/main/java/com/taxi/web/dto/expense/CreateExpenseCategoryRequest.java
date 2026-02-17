package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.ExpenseCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateExpenseCategoryRequest - Request for creating an expense category
 *
 * Validates that application type and related fields are consistent.
 * Ensures data integrity at the API boundary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseCategoryRequest {

    @NotBlank(message = "Category code is required")
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String description;

    @NotNull(message = "Category type is required")
    private ExpenseCategory.CategoryType categoryType;  // FIXED or VARIABLE

    @NotNull(message = "Applies to is required")
    private ExpenseCategory.AppliesTo appliesTo;  // CAB, SHIFT, OWNER, DRIVER, COMPANY

    @NotNull(message = "Application type is required")
    private ApplicationType applicationType;

    // Application type specific fields
    private Long shiftProfileId;
    private Long specificShiftId;
    private Long specificPersonId;
    private Long attributeTypeId;

    @Builder.Default
    private boolean isActive = true;

    /**
     * Validates that application type and related fields are consistent
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

            case SPECIFIC_PERSON:
                return specificPersonId != null;

            case SHIFTS_WITH_ATTRIBUTE:
                return attributeTypeId != null;

            case ALL_OWNERS:
            case ALL_DRIVERS:
                // No additional fields required
                return true;

            default:
                return false;
        }
    }

    /**
     * Convert request to entity
     */
    public ExpenseCategory toEntity() {
        return ExpenseCategory.builder()
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .description(description)
                .categoryType(categoryType)
                .appliesTo(appliesTo)
                .applicationType(applicationType)
                .shiftProfileId(shiftProfileId)
                .specificShiftId(specificShiftId)
                .specificPersonId(specificPersonId)
                .attributeTypeId(attributeTypeId)
                .isActive(isActive)
                .build();
    }
}
