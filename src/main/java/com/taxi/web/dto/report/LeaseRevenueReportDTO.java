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

    // List of all insurance mileage revenue items
    @Builder.Default
    private List<InsuranceMileageDTO> insuranceMileageItems = new ArrayList<>();

    // Summary totals
    private BigDecimal totalBaseRates;
    private BigDecimal totalMiles;
    private BigDecimal totalMileageLease;
    private BigDecimal grandTotalLease;
    private BigDecimal totalInsuranceMileage;  // Total insurance mileage income
    private Integer totalShifts;
    
    /**
     * Calculate all summary totals from lease and insurance items
     */
    public void calculateSummary() {
        totalBaseRates = BigDecimal.ZERO;
        totalMiles = BigDecimal.ZERO;
        totalMileageLease = BigDecimal.ZERO;
        totalInsuranceMileage = BigDecimal.ZERO;
        grandTotalLease = BigDecimal.ZERO;
        totalShifts = (leaseItems != null ? leaseItems.size() : 0);

        // Calculate lease totals
        if (leaseItems != null) {
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

        // Calculate insurance mileage totals
        if (insuranceMileageItems != null && !insuranceMileageItems.isEmpty()) {
            totalInsuranceMileage = insuranceMileageItems.stream()
                    .map(InsuranceMileageDTO::getTotalInsuranceMileage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Add a lease item to the report
     */
    public void addLeaseItem(LeaseRevenueDTO item) {
        this.leaseItems.add(item);
    }

    /**
     * Add an insurance mileage item to the report
     */
    public void addInsuranceMileage(InsuranceMileageDTO insurance) {
        if (this.insuranceMileageItems == null) {
            this.insuranceMileageItems = new ArrayList<>();
        }
        this.insuranceMileageItems.add(insurance);
    }
}
