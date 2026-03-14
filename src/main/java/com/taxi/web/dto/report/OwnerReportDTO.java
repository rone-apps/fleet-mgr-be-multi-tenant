package com.taxi.web.dto.report;

import com.taxi.domain.statement.model.StatementStatus;
import com.taxi.web.dto.expense.StatementLineItem;
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
public class OwnerReportDTO {

    private Long ownerId;
    private String ownerName;
    private String ownerNumber;

    private LocalDate periodFrom;
    private LocalDate periodTo;

    // Statement metadata
    private Long statementId;             // null if draft, set if finalized
    private String personType;            // "DRIVER" or "OWNER"
    private StatementStatus status;       // DRAFT or FINALIZED
    private BigDecimal previousBalance;   // from last finalized statement
    private BigDecimal paidAmount;        // admin-entered
    private BigDecimal netDue;            // previousBalance + totalExpenses - totalRevenues - paidAmount

    // Revenues
    @Builder.Default
    private List<RevenueLineItem> revenues = new ArrayList<>();
    private BigDecimal totalRevenues;

    // Recurring Expenses
    @Builder.Default
    private List<StatementLineItem> recurringExpenses = new ArrayList<>();
    private BigDecimal totalRecurringExpenses;

    // One-Time Expenses
    @Builder.Default
    private List<StatementLineItem> oneTimeExpenses = new ArrayList<>();
    private BigDecimal totalOneTimeExpenses;

    // Per-Unit Expenses (mileage, airport trips, etc.)
    @Builder.Default
    private List<PerUnitExpenseLineItem> perUnitExpenses = new ArrayList<>();
    private BigDecimal totalPerUnitExpenses;

    // Insurance Mileage (always an expense, deducted from driver earnings)
    @Builder.Default
    private List<StatementLineItem> insuranceMileageExpenses = new ArrayList<>();  // For drivers
    private BigDecimal totalInsuranceMileageExpenses;

    // Airport Trip Expenses (separate from one-time expenses)
    @Builder.Default
    private List<StatementLineItem> airportTripExpenses = new ArrayList<>();
    private BigDecimal totalAirportTripExpenses;

    // Tax on Expenses (calculated from tax assignments)
    @Builder.Default
    private List<TaxLineItem> taxExpenses = new ArrayList<>();
    private BigDecimal totalTaxExpenses;

    // Commission on Revenue (calculated from commission assignments)
    @Builder.Default
    private List<CommissionLineItem> commissionExpenses = new ArrayList<>();
    private BigDecimal totalCommissionExpenses;

    // Totals
    private BigDecimal totalExpenses;
    private BigDecimal netAmount;  // Revenues - Expenses

    public void calculateTotals() {
        totalRevenues = revenues.stream()
                .map(RevenueLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalRecurringExpenses = recurringExpenses.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalOneTimeExpenses = oneTimeExpenses.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPerUnitExpenses = perUnitExpenses.stream()
                .map(PerUnitExpenseLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add insurance mileage expenses to total expenses
        BigDecimal insuranceMileageExp = insuranceMileageExpenses.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalInsuranceMileageExpenses = insuranceMileageExp;

        // Add airport trip expenses to total expenses
        BigDecimal airportTripExp = airportTripExpenses.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAirportTripExpenses = airportTripExp;

        BigDecimal taxExp = taxExpenses.stream()
                .map(TaxLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalTaxExpenses = taxExp;

        BigDecimal commissionExp = commissionExpenses.stream()
                .map(CommissionLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCommissionExpenses = commissionExp;

        totalExpenses = totalRecurringExpenses.add(totalOneTimeExpenses).add(totalPerUnitExpenses)
                .add(insuranceMileageExp).add(airportTripExp).add(taxExp).add(commissionExp);
        netAmount = totalRevenues.subtract(totalExpenses);

        // Calculate netDue: previousBalance + revenues - expenses - paidAmount
        // (what driver is owed from previous period + this period's earnings - deductions - what was paid)
        BigDecimal prevBalance = previousBalance != null ? previousBalance : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        netDue = prevBalance.add(totalRevenues).subtract(totalExpenses).subtract(paid);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueLineItem {
        private String categoryName;
        private LocalDate revenueDate;
        private String description;
        private String revenueType;
        private String revenueSubType; // "LEASE_INCOME", "OTHER_REVENUE", "ACCOUNT_REVENUE", etc.
        private BigDecimal amount;

        // Cab number for credit card and account charge revenues
        private String cabNumber;

        // Additional fields for account charges and other revenues
        private String accountName;        // For account charges: company name
        private String pickupAddress;      // For account charges: pickup location
        private String dropoffAddress;     // For account charges: dropoff location
        private BigDecimal tipAmount;      // For account charges: tip
        private BigDecimal fareAmount;     // For account charges: base fare (same as amount)

        // ✅ NEW: Application type display for revenues
        private String applicationTypeDisplay;  // e.g., "All Drivers", "Specific Shift", "Shift Profile"

        // For insurance mileage and mileage-based revenues
        private BigDecimal miles;  // Miles driven (for insurance calculations, etc.)

        // For lease revenue breakdown: Fixed Lease | Mileage Lease | Total Lease
        private StatementLineItem.LeaseBreakdown leaseBreakdown;

        // Commission fields for credit card revenues
        private BigDecimal commissionRate;     // e.g., 2.00 for 2%
        private BigDecimal commissionAmount;   // amount × rate / 100
        private BigDecimal netAmount;          // amount - commissionAmount
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerUnitExpenseLineItem {
        private String name;                    // e.g., "Per Mile Insurance"
        private String unitType;                // e.g., "MILEAGE", "AIRPORT_TRIP"
        private String unitTypeDisplay;         // e.g., "Miles driven", "Airport trips"
        private BigDecimal totalUnits;          // Total miles or trips in the period
        private BigDecimal rate;                // Rate per unit
        private BigDecimal amount;              // totalUnits × rate
        private String chargedTo;               // "DRIVER" or "OWNER"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxLineItem {
        private String taxTypeCode;             // e.g., "HST"
        private String taxTypeName;             // e.g., "Harmonized Sales Tax"
        private BigDecimal taxRate;             // e.g., 13.00 (%)
        private String expenseCategoryName;     // e.g., "Dispatch Fee"
        private BigDecimal baseAmount;          // amount before tax
        private BigDecimal amount;              // tax amount = baseAmount × rate / 100
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommissionLineItem {
        private String commissionTypeCode;      // e.g., "CC_COMMISSION"
        private String commissionTypeName;      // e.g., "Credit Card Commission"
        private BigDecimal commissionRate;       // e.g., 2.00 (%)
        private String revenueCategoryName;     // e.g., "Credit Card Revenue"
        private BigDecimal baseAmount;          // revenue amount
        private BigDecimal amount;              // commission = baseAmount × rate / 100
    }
}
