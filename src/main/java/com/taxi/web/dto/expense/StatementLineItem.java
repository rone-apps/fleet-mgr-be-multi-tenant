package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense.BillingMethod;
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
public class StatementLineItem {

    private String categoryCode;
    private String categoryName;
    private String applicationType;
    private String entityDescription;  // e.g., "Cab 101 - DAY shift" or "All Active Shifts"

    // For recurring expenses
    private BillingMethod billingMethod;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // For one-time expenses
    private LocalDate date;
    private String description;

    private BigDecimal amount;
    private boolean isRecurring;
}
