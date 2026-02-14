package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense.BillingMethod;
import jakarta.validation.constraints.NotNull;
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
public class CreateRecurringExpenseRequest {

    @NotNull(message = "Expense category ID is required")
    private Long expenseCategoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Billing method is required")
    private BillingMethod billingMethod;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String notes;
}
