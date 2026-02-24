package com.taxi.web.dto.expense;

import com.taxi.domain.statement.model.StatementStatus;
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

    // Insurance Mileage (separate tab like Lease)
    @Builder.Default
    private List<StatementLineItem> insuranceMileageExpenses = new ArrayList<>();  // For drivers
    private BigDecimal totalInsuranceMileageExpenses;

    @Builder.Default
    private List<RevenueLineItem> insuranceMileageRevenues = new ArrayList<>();   // For owners
    private BigDecimal totalInsuranceMileageRevenues;

    // Totals
    private BigDecimal totalExpenses;
    private BigDecimal netAmount;  // Revenues - Expenses

    public void calculateTotals() {
        totalRevenues = revenues.stream()
                .map(RevenueLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add insurance mileage revenues to total revenues
        BigDecimal insuranceMileageRev = insuranceMileageRevenues.stream()
                .map(RevenueLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalInsuranceMileageRevenues = insuranceMileageRev;
        totalRevenues = totalRevenues.add(insuranceMileageRev);

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

        totalExpenses = totalRecurringExpenses.add(totalOneTimeExpenses).add(totalPerUnitExpenses).add(insuranceMileageExp);
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
}
