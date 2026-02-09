package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating recurring expenses
 * Application type and entity details are determined by the selected expense category
 */
@Data
public class RecurringExpenseRequest {

    private Long expenseCategoryId;  // Required - determines application type and entity mapping
    private BigDecimal amount;
    private RecurringExpense.BillingMethod billingMethod;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
    private Boolean isActive;
}