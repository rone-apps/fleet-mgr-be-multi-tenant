package com.taxi.web.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatementDTO {

    private Long driverId;
    private String driverName;
    private String driverNumber;

    private LocalDate periodFrom;
    private LocalDate periodTo;

    @Builder.Default
    private List<StatementLineItem> recurringCharges = new ArrayList<>();

    @Builder.Default
    private List<StatementLineItem> oneTimeCharges = new ArrayList<>();

    // Per-unit mileage expenses (breakdown by lease vs mileage-based)
    @Builder.Default
    private List<MileageExpenseLineItem> mileageExpenses = new ArrayList<>();

    private BigDecimal recurringTotal;
    private BigDecimal oneTimeTotal;
    private BigDecimal mileageExpenseTotal;
    private BigDecimal grandTotal;

    public void calculateTotals() {
        recurringTotal = recurringCharges.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        oneTimeTotal = oneTimeCharges.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        mileageExpenseTotal = mileageExpenses.stream()
                .map(MileageExpenseLineItem::getTotalLeaseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        grandTotal = recurringTotal.add(oneTimeTotal).add(mileageExpenseTotal);
    }

    /**
     * Inner class for mileage-based expenses with breakdown
     * Shows: Fixed Lease | Mileage Lease (miles × rate) | Total Lease
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MileageExpenseLineItem {
        private String itemRateName;      // e.g., "Per Mile Insurance"
        private String unitType;          // MILEAGE, AIRPORT_TRIP
        private BigDecimal totalUnits;    // Total miles or airport trips
        private BigDecimal fixedLeaseAmount; // Fixed lease amount already being charged
        private BigDecimal mileageRate;   // Per-unit rate for mileage
        private BigDecimal mileageLeaseAmount; // mileageRate × totalUnits
        private BigDecimal totalLeaseAmount;  // fixedLeaseAmount + mileageLeaseAmount
    }
}
