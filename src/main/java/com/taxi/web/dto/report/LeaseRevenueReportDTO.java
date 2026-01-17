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
 * DTO for complete lease revenue report
 * Contains all shifts and summary totals for a cab owner
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseRevenueReportDTO {
    
    // Report parameters
    private String ownerDriverNumber;
    private String ownerDriverName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // List of all lease revenue items
    @Builder.Default
    private List<LeaseRevenueDTO> leaseItems = new ArrayList<>();
    
    // Summary totals
    private BigDecimal totalBaseRates;
    private BigDecimal totalMiles;
    private BigDecimal totalMileageLease;
    private BigDecimal grandTotalLease;
    private Integer totalShifts;
    
    /**
     * Calculate all summary totals from lease items
     */
    public void calculateSummary() {
        totalBaseRates = BigDecimal.ZERO;
        totalMiles = BigDecimal.ZERO;
        totalMileageLease = BigDecimal.ZERO;
        grandTotalLease = BigDecimal.ZERO;
        totalShifts = leaseItems.size();
        
        for (LeaseRevenueDTO item : leaseItems) {
            if (item.getBaseRate() != null) {
                totalBaseRates = totalBaseRates.add(item.getBaseRate());
            }
            if (item.getMiles() != null) {
                totalMiles = totalMiles.add(item.getMiles());
            }
            if (item.getMileageLease() != null) {
                totalMileageLease = totalMileageLease.add(item.getMileageLease());
            }
            if (item.getTotalLease() != null) {
                grandTotalLease = grandTotalLease.add(item.getTotalLease());
            }
        }
    }
    
    /**
     * Add a lease item to the report
     */
    public void addLeaseItem(LeaseRevenueDTO item) {
        this.leaseItems.add(item);
    }
}
