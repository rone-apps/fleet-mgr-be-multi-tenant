package com.taxi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for driver financial summary
 * Contains all revenue and expense metrics for a driver in a given period
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSummaryDTO {

    // Driver identification
    private Long driverId;                 // ✅ Driver ID for fetching detailed reports
    private String driverNumber;
    private String driverName;
    private Boolean isOwner;
    
    // Revenue metrics
    private BigDecimal leaseRevenue;           // Revenue earned from owning shifts
    private BigDecimal creditCardRevenue;      // Revenue from credit card transactions
    private BigDecimal chargesRevenue;         // Revenue from account charges
    private BigDecimal otherRevenue;           // Other miscellaneous revenue
    
    // Expense metrics
    private BigDecimal fixedExpense;           // Fixed expenses (from OneTimeExpense/RecurringExpense)
    private BigDecimal leaseExpense;           // Lease paid to shift owners
    private BigDecimal variableExpense;        // Variable expenses
    private BigDecimal otherExpense;           // Other miscellaneous expenses
    
    // Financial totals
    private BigDecimal totalRevenue;           // Sum of all revenues
    private BigDecimal totalExpense;           // Sum of all expenses
    private BigDecimal netOwed;                // Total revenue - total expense
    
    // Payment tracking
    private BigDecimal paid;                   // Amount already paid
    private BigDecimal outstanding;            // Amount still owed (netOwed - paid)
    
    /**
     * Calculate total revenue from all sources
     */
    public void calculateTotalRevenue() {
        this.totalRevenue = safeAdd(leaseRevenue, creditCardRevenue, chargesRevenue, otherRevenue);
    }
    
    /**
     * Calculate total expense from all categories
     */
    public void calculateTotalExpense() {
        this.totalExpense = safeAdd(fixedExpense, leaseExpense, variableExpense, otherExpense);
    }
    
    /**
     * Calculate net owed (revenue - expense)
     */
    public void calculateNetOwed() {
        this.netOwed = safeBigDecimal(totalRevenue).subtract(safeBigDecimal(totalExpense));
    }
    
    /**
     * Calculate outstanding balance (netOwed - paid)
     */
    public void calculateOutstanding() {
        this.outstanding = safeBigDecimal(netOwed).subtract(safeBigDecimal(paid));
    }
    
    /**
     * Calculate all derived fields
     */
    public void calculateAll() {
        calculateTotalRevenue();
        calculateTotalExpense();
        calculateNetOwed();
        calculateOutstanding();
    }
    
    /**
     * Helper method to safely add BigDecimal values (treating null as zero)
     */
    private BigDecimal safeAdd(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
            }
        }
        return sum;
    }
    
    /**
     * Helper method to safely handle null BigDecimal (returns ZERO if null)
     */
    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}