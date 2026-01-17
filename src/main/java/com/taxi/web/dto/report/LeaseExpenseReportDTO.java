package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for complete lease expense report
 * Contains all shifts where a driver worked shifts owned by others
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseExpenseReportDTO {
    
    // Report metadata
    private String workingDriverNumber;  // Driver who worked the shifts
    private String workingDriverName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Detailed items
    @Builder.Default
    private List<LeaseExpenseDTO> leaseExpenseItems = new ArrayList<>();
    
    // Summary totals
    private Integer totalShifts;
    private BigDecimal totalMiles;
    private BigDecimal totalBaseRates;
    private BigDecimal totalMileageLease;
    private BigDecimal grandTotalLease;
    
    /**
     * Add a lease expense item to the report
     */
    public void addLeaseExpense(LeaseExpenseDTO expense) {
        if (leaseExpenseItems == null) {
            leaseExpenseItems = new ArrayList<>();
        }
        leaseExpenseItems.add(expense);
    }
    
    /**
     * Calculate summary totals from all items
     */
    public void calculateSummary() {
        if (leaseExpenseItems == null || leaseExpenseItems.isEmpty()) {
            totalShifts = 0;
            totalMiles = BigDecimal.ZERO;
            totalBaseRates = BigDecimal.ZERO;
            totalMileageLease = BigDecimal.ZERO;
            grandTotalLease = BigDecimal.ZERO;
            return;
        }
        
        totalShifts = leaseExpenseItems.size();
        
        totalMiles = leaseExpenseItems.stream()
                .map(LeaseExpenseDTO::getMiles)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalBaseRates = leaseExpenseItems.stream()
                .map(LeaseExpenseDTO::getBaseRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalMileageLease = leaseExpenseItems.stream()
                .map(LeaseExpenseDTO::getMileageLease)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        grandTotalLease = leaseExpenseItems.stream()
                .map(LeaseExpenseDTO::getTotalLease)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}