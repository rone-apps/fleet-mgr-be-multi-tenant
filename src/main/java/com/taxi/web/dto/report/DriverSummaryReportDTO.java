package com.taxi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for driver summary report containing multiple driver summaries
 * Now includes pagination support and cumulative totals
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSummaryReportDTO {
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    @Builder.Default
    private List<DriverSummaryDTO> driverSummaries = new ArrayList<>();
    
    // Pagination fields
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer pageSize;
    
    // Page totals (for current page only)
    private BigDecimal pageLeaseRevenue;
    private BigDecimal pageCreditCardRevenue;
    private BigDecimal pageChargesRevenue;
    private BigDecimal pageOtherRevenue;
    private BigDecimal pageFixedExpense;
    private BigDecimal pageLeaseExpense;
    private BigDecimal pageVariableExpense;
    private BigDecimal pageOtherExpense;
    private BigDecimal pageTotalRevenue;
    private BigDecimal pageTotalExpense;
    private BigDecimal pageNetOwed;
    private BigDecimal pageTotalPaid;
    private BigDecimal pageTotalOutstanding;
    
    // Cumulative totals (from page 0 up to and including current page)
    private BigDecimal cumulativeLeaseRevenue;
    private BigDecimal cumulativeCreditCardRevenue;
    private BigDecimal cumulativeChargesRevenue;
    private BigDecimal cumulativeOtherRevenue;
    private BigDecimal cumulativeFixedExpense;
    private BigDecimal cumulativeLeaseExpense;
    private BigDecimal cumulativeVariableExpense;
    private BigDecimal cumulativeOtherExpense;
    private BigDecimal cumulativeTotalRevenue;
    private BigDecimal cumulativeTotalExpense;
    private BigDecimal cumulativeNetOwed;
    private BigDecimal cumulativeTotalPaid;
    private BigDecimal cumulativeTotalOutstanding;
    private Integer cumulativeDriverCount;
    
    // Grand totals (ALL pages - only populated on last page)
    private BigDecimal grandTotalLeaseRevenue;
    private BigDecimal grandTotalCreditCardRevenue;
    private BigDecimal grandTotalChargesRevenue;
    private BigDecimal grandTotalOtherRevenue;
    private BigDecimal grandTotalFixedExpense;
    private BigDecimal grandTotalLeaseExpense;
    private BigDecimal grandTotalVariableExpense;
    private BigDecimal grandTotalOtherExpense;
    private BigDecimal grandTotalRevenue;
    private BigDecimal grandTotalExpense;
    private BigDecimal grandNetOwed;
    private BigDecimal grandTotalPaid;
    private BigDecimal grandTotalOutstanding;
    
    private Integer totalDrivers;  // Total drivers in current page
    
    /**
     * Add a driver summary to the report
     */
    public void addDriverSummary(DriverSummaryDTO summary) {
        this.driverSummaries.add(summary);
    }
    
    /**
     * Calculate page totals from all driver summaries in current page
     */
    public void calculatePageTotals() {
        pageLeaseRevenue = BigDecimal.ZERO;
        pageCreditCardRevenue = BigDecimal.ZERO;
        pageChargesRevenue = BigDecimal.ZERO;
        pageOtherRevenue = BigDecimal.ZERO;
        pageFixedExpense = BigDecimal.ZERO;
        pageLeaseExpense = BigDecimal.ZERO;
        pageVariableExpense = BigDecimal.ZERO;
        pageOtherExpense = BigDecimal.ZERO;
        pageTotalRevenue = BigDecimal.ZERO;
        pageTotalExpense = BigDecimal.ZERO;
        pageNetOwed = BigDecimal.ZERO;
        pageTotalPaid = BigDecimal.ZERO;
        pageTotalOutstanding = BigDecimal.ZERO;
        
        for (DriverSummaryDTO summary : driverSummaries) {
            pageLeaseRevenue = pageLeaseRevenue.add(safeBigDecimal(summary.getLeaseRevenue()));
            pageCreditCardRevenue = pageCreditCardRevenue.add(safeBigDecimal(summary.getCreditCardRevenue()));
            pageChargesRevenue = pageChargesRevenue.add(safeBigDecimal(summary.getChargesRevenue()));
            pageOtherRevenue = pageOtherRevenue.add(safeBigDecimal(summary.getOtherRevenue()));
            pageFixedExpense = pageFixedExpense.add(safeBigDecimal(summary.getFixedExpense()));
            pageLeaseExpense = pageLeaseExpense.add(safeBigDecimal(summary.getLeaseExpense()));
            pageVariableExpense = pageVariableExpense.add(safeBigDecimal(summary.getVariableExpense()));
            pageOtherExpense = pageOtherExpense.add(safeBigDecimal(summary.getOtherExpense()));
            pageTotalRevenue = pageTotalRevenue.add(safeBigDecimal(summary.getTotalRevenue()));
            pageTotalExpense = pageTotalExpense.add(safeBigDecimal(summary.getTotalExpense()));
            pageNetOwed = pageNetOwed.add(safeBigDecimal(summary.getNetOwed()));
            pageTotalPaid = pageTotalPaid.add(safeBigDecimal(summary.getPaid()));
            pageTotalOutstanding = pageTotalOutstanding.add(safeBigDecimal(summary.getOutstanding()));
        }
        
        totalDrivers = driverSummaries.size();
    }
    
    /**
     * Calculate grand totals (alias for calculatePageTotals for non-paginated reports)
     */
    public void calculateGrandTotals() {
        calculatePageTotals();
    }
    
    /**
     * Helper method to safely handle null BigDecimal
     */
    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}