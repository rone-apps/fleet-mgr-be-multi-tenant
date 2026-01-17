package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.shift.model.ShiftType;  // ✅ USE SHARED ENUM
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating recurring expenses
 */
@Data
public class RecurringExpenseRequest {
    
    private Long expenseCategoryId;
    private RecurringExpense.EntityType entityType;
    private Long entityId;
    private ShiftType shiftType;  // ✅ For SHIFT entity type
    private BigDecimal amount;
    private RecurringExpense.BillingMethod billingMethod;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
    private Boolean isActive;
}