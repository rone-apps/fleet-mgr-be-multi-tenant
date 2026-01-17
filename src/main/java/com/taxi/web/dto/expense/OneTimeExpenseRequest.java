package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.shift.model.ShiftType;  // ✅ ADDED
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating one-time expenses
 */
@Data
public class OneTimeExpenseRequest {
    
    private Long expenseCategoryId;
    private OneTimeExpense.EntityType entityType;
    private Long entityId;
    private ShiftType shiftType;  // ✅ ADDED - For SHIFT entity type
    private BigDecimal amount;
    private LocalDate expenseDate;
    private OneTimeExpense.PaidBy paidBy;
    private OneTimeExpense.ResponsibleParty responsibleParty;
    private String description;
    private String vendor;
    private String receiptUrl;
    private String invoiceNumber;
    private Boolean isReimbursable;
    private String notes;
}