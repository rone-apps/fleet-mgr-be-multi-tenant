package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ExpenseCreationResultDTO - Result of creating expenses for a category
 *
 * Provides feedback on how many expenses were created when applying
 * an expense category to its target entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCreationResultDTO {

    /**
     * Number of expenses successfully created
     */
    private int createdCount;

    /**
     * Total number of expenses expected
     * (may differ from created count if some failed)
     */
    private int totalCount;

    /**
     * Summary of created expenses
     * Limited to first 10 for response size management
     */
    private List<ExpenseSummary> createdExpenses;

    /**
     * Any errors encountered during creation
     */
    private List<String> errors;

    /**
     * Whether all expenses were created successfully
     */
    private boolean success;

    /**
     * Simple summary of a created expense
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseSummary {
        private Long id;
        private String entityType;  // CAB, SHIFT, OWNER, DRIVER
        private String entityName;
        private String entityIdentifier;
        private String amount;
        private RecurringExpense.BillingMethod billingMethod;
        private String status;  // CREATED, ACTIVE, etc.
    }
}
