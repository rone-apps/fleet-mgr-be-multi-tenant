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

        totalExpenses = totalRecurringExpenses.add(totalOneTimeExpenses);
        netAmount = totalRevenues.subtract(totalExpenses);

        // Calculate netDue: previousBalance + expenses - revenues - paidAmount
        BigDecimal prevBalance = previousBalance != null ? previousBalance : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        netDue = prevBalance.add(totalExpenses).subtract(totalRevenues).subtract(paid);
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
    }
}
