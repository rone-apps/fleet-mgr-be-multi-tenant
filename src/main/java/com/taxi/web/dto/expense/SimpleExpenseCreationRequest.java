package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SimpleExpenseCreationRequest - Simplified request for creating expenses for a category
 *
 * Replaces complex auto-apply logic with straightforward expense creation.
 * The category's application type determines which entities receive the expense.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleExpenseCreationRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Billing method is required")
    private RecurringExpense.BillingMethod billingMethod;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    /**
     * Optional end date for the expense
     * If not provided, expense is open-ended
     */
    private LocalDate effectiveTo;

    /**
     * Optional notes for this expense
     */
    private String notes;
}
