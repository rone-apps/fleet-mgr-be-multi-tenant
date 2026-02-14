package com.taxi.web.dto.expense;

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
public class AnalyticsDTO {

    private LocalDate periodFrom;
    private LocalDate periodTo;

    // Expense Metrics
    private BigDecimal totalRecurringExpenses;
    private BigDecimal totalOneTimeExpenses;
    private BigDecimal totalExpenses;

    // Count Metrics
    private Long activeDriverCount;
    private Long activeOwnerCount;

    // TODO: Revenue metrics when revenue repository is integrated
    // private BigDecimal totalRevenues;
    // private BigDecimal netAmount;
}
