package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for complete fixed expense report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpenseReportDTO {
    
    // Report metadata
    private String driverNumber;  // Can be null for "all drivers" report
    private String driverName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Expense items
    @Builder.Default
    private List<FixedExpenseItemDTO> expenseItems = new ArrayList<>();
    
    // Summary by category
    @Builder.Default
    private List<CategorySummaryDTO> categorySummaries = new ArrayList<>();
    
    // Totals
    private Integer totalExpenses;
    private Integer recurringExpenses;
    private Integer oneTimeExpenses;
    private BigDecimal totalAmount;
    private BigDecimal recurringAmount;
    private BigDecimal oneTimeAmount;
    
    /**
     * Add an expense item to the report
     */
    public void addExpenseItem(FixedExpenseItemDTO item) {
        if (expenseItems == null) {
            expenseItems = new ArrayList<>();
        }
        expenseItems.add(item);
    }
    
    /**
     * Calculate summary totals
     */
    public void calculateSummary() {
        if (expenseItems == null || expenseItems.isEmpty()) {
            totalExpenses = 0;
            recurringExpenses = 0;
            oneTimeExpenses = 0;
            totalAmount = BigDecimal.ZERO;
            recurringAmount = BigDecimal.ZERO;
            oneTimeAmount = BigDecimal.ZERO;
            return;
        }
        
        totalExpenses = expenseItems.size();
        
        recurringExpenses = (int) expenseItems.stream()
                .filter(item -> "RECURRING".equals(item.getExpenseType()))
                .count();
        
        oneTimeExpenses = (int) expenseItems.stream()
                .filter(item -> "ONE_TIME".equals(item.getExpenseType()))
                .count();
        
        totalAmount = expenseItems.stream()
                .map(FixedExpenseItemDTO::getChargedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        recurringAmount = expenseItems.stream()
                .filter(item -> "RECURRING".equals(item.getExpenseType()))
                .map(FixedExpenseItemDTO::getChargedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        oneTimeAmount = expenseItems.stream()
                .filter(item -> "ONE_TIME".equals(item.getExpenseType()))
                .map(FixedExpenseItemDTO::getChargedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Category summary DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummaryDTO {
        private String category;
        private Integer count;
        private BigDecimal amount;
    }
}