package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for auto-applying expenses to matching cabs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoApplyRequest {

    private BigDecimal amount;

    private RecurringExpense.BillingMethod billingMethod;

    private LocalDate effectiveFrom;
}
