package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for bulk creating expenses with individual amounts per cab
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateRequest {

    /**
     * Map of cabId to amount
     */
    private Map<Long, BigDecimal> cabAmounts;

    private RecurringExpense.BillingMethod billingMethod;

    private LocalDate effectiveFrom;
}
