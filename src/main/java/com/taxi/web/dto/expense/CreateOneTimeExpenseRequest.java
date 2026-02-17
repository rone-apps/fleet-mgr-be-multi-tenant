package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.OneTimeExpense.PaidBy;
import com.taxi.domain.expense.model.OneTimeExpense.ResponsibleParty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOneTimeExpenseRequest {

    /**
     * Optional expense category ID. If not provided, this becomes a standalone one-time expense
     * with no category association. Useful for ad-hoc expenses like tickets, violations, repairs, etc.
     */
    private Long expenseCategoryId;

    @NotBlank(message = "Expense name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Paid by is required")
    private PaidBy paidBy;

    private ResponsibleParty responsibleParty;

    private String description;

    private String vendor;

    private String receiptUrl;

    private String invoiceNumber;

    private boolean isReimbursable;

    private String notes;

    // ========== Application Type & Targeting ==========

    /**
     * Determines how this expense is applied to entities:
     * - SHIFT_PROFILE: Apply to all shifts with a specific profile
     * - SPECIFIC_SHIFT: Apply to one specific shift
     * - SPECIFIC_PERSON: Apply to a specific driver or owner
     * - SHIFTS_WITH_ATTRIBUTE: Apply to all shifts with a specific attribute
     * - ALL_OWNERS: Apply to all owners
     * - ALL_DRIVERS: Apply to all drivers
     */
    private ApplicationType applicationType;

    /**
     * Shift profile ID - required if applicationType = SHIFT_PROFILE
     */
    private Long shiftProfileId;

    /**
     * Specific shift ID - required if applicationType = SPECIFIC_SHIFT
     */
    private Long specificShiftId;

    /**
     * Specific person ID (driver or owner) - required if applicationType = SPECIFIC_PERSON
     */
    private Long specificPersonId;

    /**
     * Attribute type ID - required if applicationType = SHIFTS_WITH_ATTRIBUTE
     * This charges the expense to all shifts that have this attribute assigned
     */
    private Long attributeTypeId;
}
