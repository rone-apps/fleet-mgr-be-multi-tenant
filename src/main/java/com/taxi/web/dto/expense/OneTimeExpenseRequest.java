package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.ApplicationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating one-time expenses
 * Charges to entities via application type system (shift profile, specific shift, owner/driver, etc.)
 */
@Data
public class OneTimeExpenseRequest {

    // Expense details
    private String name;  // Required - name/title of the expense
    private BigDecimal amount;  // Required
    private LocalDate expenseDate;  // Required
    private OneTimeExpense.PaidBy paidBy;  // Required

    // Application type system - determines who to charge this expense to
    private ApplicationType applicationType;  // Optional - SHIFT_PROFILE, SPECIFIC_SHIFT, SPECIFIC_PERSON, SHIFTS_WITH_ATTRIBUTE, ALL_OWNERS, ALL_DRIVERS
    private Long shiftProfileId;  // For SHIFT_PROFILE type
    private Long specificShiftId;  // For SPECIFIC_SHIFT type
    private Long specificPersonId;  // For SPECIFIC_PERSON type (driver or owner)
    private Long attributeTypeId;  // For SHIFTS_WITH_ATTRIBUTE type

    // Additional expense details
    private OneTimeExpense.ResponsibleParty responsibleParty;
    private String description;
    private String vendor;
    private String receiptUrl;
    private String invoiceNumber;
    private Boolean isReimbursable;
    private String notes;
}