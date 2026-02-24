package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.RecurringExpense.BillingMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementLineItem {

    private String categoryCode;
    private String categoryName;
    private String applicationType;
    private String entityDescription;  // e.g., "Cab 101 - DAY shift" or "All Active Shifts"

    // ✅ NEW: Explicit fields for shift-specific charges (easier parsing on frontend)
    private String cabNumber;      // e.g., "17"
    private String shiftType;      // e.g., "Day Shift", "Night Shift"
    private String chargeTarget;   // e.g., "With Attribute", "All Active Shifts"

    // For recurring expenses
    private BillingMethod billingMethod;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // For one-time expenses
    private LocalDate date;
    private String description;

    private BigDecimal amount;
    private boolean isRecurring;

    // For insurance mileage and mileage-based charges
    private BigDecimal miles;  // Miles driven (for insurance calculations, etc.)

    // For lease expenses breakdown: Fixed Lease | Mileage Lease | Total Lease
    private LeaseBreakdown leaseBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaseBreakdown {
        private BigDecimal fixedLeaseAmount;    // Base lease amount (e.g., $50/day)
        private BigDecimal mileageLeaseAmount;  // Mileage portion (miles × rate)
        // Total is in the parent amount field
    }
}
