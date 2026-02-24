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
    
    // Detailed items - Lease
    @Builder.Default
    private List<LeaseExpenseDTO> leaseExpenseItems = new ArrayList<>();

    // Detailed items - Insurance Mileage
    @Builder.Default
    private List<InsuranceMileageDTO> insuranceMileageItems = new ArrayList<>();

    // Summary totals
    private Integer totalShifts;
    private BigDecimal totalMiles;
    private BigDecimal totalBaseRates;
    private BigDecimal totalMileageLease;
    private BigDecimal grandTotalLease;
    private BigDecimal totalInsuranceMileage;  // Total insurance mileage amount
    
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
     * Add an insurance mileage item to the report
     */
    public void addInsuranceMileage(InsuranceMileageDTO insurance) {
        if (insuranceMileageItems == null) {
            insuranceMileageItems = new ArrayList<>();
        }
        insuranceMileageItems.add(insurance);
    }
    
    /**
     * Calculate summary totals from all items
     */
    public void calculateSummary() {
        totalShifts = (leaseExpenseItems != null ? leaseExpenseItems.size() : 0);
        totalMiles = BigDecimal.ZERO;
        totalBaseRates = BigDecimal.ZERO;
        totalMileageLease = BigDecimal.ZERO;
        totalInsuranceMileage = BigDecimal.ZERO;
        grandTotalLease = BigDecimal.ZERO;

        // Calculate lease totals
        if (leaseExpenseItems != null && !leaseExpenseItems.isEmpty()) {
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

        // Calculate insurance mileage totals
        if (insuranceMileageItems != null && !insuranceMileageItems.isEmpty()) {
            totalInsuranceMileage = insuranceMileageItems.stream()
                    .map(InsuranceMileageDTO::getTotalInsuranceMileage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}