package com.taxi.domain.shift.model;

import com.taxi.domain.expense.model.ShiftExpense;
import com.taxi.domain.revenue.model.Revenue;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root representing a shift operation transaction
 * Records all financial activity for a shift on a specific date
 * 
 * Business Rule: If driver == owner, no lease charged (owner drives own shift)
 * Business Rule: Total Lease = Base Rate + (Total Miles × Mileage Rate)
 */
@Entity
@Table(name = "shift_log", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"cab_id", "shift_id", "log_date"}),
       indexes = {
           @Index(name = "idx_log_date", columnList = "log_date"),
           @Index(name = "idx_owner_date", columnList = "owner_id, log_date"),
           @Index(name = "idx_settlement", columnList = "settlement_status, log_date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cab", "shift", "owner", "segments", "revenues", "expenses"})
@EqualsAndHashCode(of = "id")
public class ShiftLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id", nullable = false)
    private Cab cab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private CabShift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Driver owner;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    // Mileage information
    @Column(name = "start_meter_reading", precision = 10, scale = 2)
    private BigDecimal startMeterReading;

    @Column(name = "end_meter_reading", precision = 10, scale = 2)
    private BigDecimal endMeterReading;

    @Column(name = "total_miles", precision = 10, scale = 2)
    private BigDecimal totalMiles;

    // Lease calculation (snapshot for historical accuracy)
    @Embedded
    private LeaseCalculation leaseCalculation;

    // Financial summary
    @Embedded
    private FinancialSummary financialSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    // Child entities
    @OneToMany(mappedBy = "shiftLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DriverSegment> segments = new ArrayList<>();

    @OneToMany(mappedBy = "shiftLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Revenue> revenues = new ArrayList<>();

    @OneToMany(mappedBy = "shiftLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShiftExpense> expenses = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Business logic: Add a driver segment (for handovers)
     */
    public void addSegment(DriverSegment segment) {
        segments.add(segment);
        segment.setShiftLog(this);
    }

    /**
     * Business logic: Add revenue
     */
    public void addRevenue(Revenue revenue) {
        revenues.add(revenue);
        revenue.setShiftLog(this);
    }

    /**
     * Business logic: Add expense
     */
    public void addExpense(ShiftExpense expense) {
        expenses.add(expense);
        expense.setShiftLog(this);
    }

    /**
     * Business logic: Calculate total miles from meter readings
     */
    public void calculateTotalMiles() {
        if (startMeterReading != null && endMeterReading != null) {
            this.totalMiles = endMeterReading.subtract(startMeterReading);
        }
    }

    /**
     * Business logic: Calculate financial summary
     */
    public void calculateFinancials() {
        BigDecimal totalRevenue = revenues.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenses.stream()
                .map(ShiftExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netRevenue = totalRevenue.subtract(totalExpenses);

        // Check if owner is driving own shift (no lease)
        boolean isOwnerDriving = segments.stream()
                .anyMatch(seg -> seg.getDriver().getId().equals(owner.getId()));

        BigDecimal totalLeaseAmount = (isOwnerDriving && segments.size() == 1) 
                ? BigDecimal.ZERO 
                : (leaseCalculation != null ? leaseCalculation.getTotalLeaseAmount() : BigDecimal.ZERO);

        BigDecimal ownerEarnings = isOwnerDriving && segments.size() == 1
                ? netRevenue  // Owner keeps everything
                : totalLeaseAmount;  // Owner gets lease amount

        BigDecimal driverEarnings = netRevenue.subtract(totalLeaseAmount);

        this.financialSummary = FinancialSummary.builder()
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netRevenue(netRevenue)
                .totalLeaseAmount(totalLeaseAmount)
                .ownerEarnings(ownerEarnings)
                .driverEarnings(driverEarnings)
                .build();
    }

    /**
     * Business logic: Complete shift and settle
     */
    public void completeAndSettle() {
        calculateTotalMiles();
        calculateFinancials();
        this.settlementStatus = SettlementStatus.SETTLED;
        this.settledAt = LocalDateTime.now();
    }

    /**
     * Business logic: Mark as disputed
     */
    public void markAsDisputed(String reason) {
        this.settlementStatus = SettlementStatus.DISPUTED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "DISPUTED: " + reason;
    }

    public enum SettlementStatus {
        PENDING,    // Shift in progress or not yet settled
        SETTLED,    // Financials calculated and settled
        DISPUTED    // Financial dispute requiring resolution
    }

    /**
     * Embedded value object for lease calculation details
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaseCalculation {
        @Column(name = "lease_base_rate", precision = 10, scale = 2)
        private BigDecimal baseRate;

        @Column(name = "lease_mileage_rate", precision = 10, scale = 4)
        private BigDecimal mileageRate;

        @Column(name = "lease_mileage_charge", precision = 10, scale = 2)
        private BigDecimal mileageCharge;

        @Column(name = "lease_total_amount", precision = 10, scale = 2)
        private BigDecimal totalLeaseAmount;

        @Column(name = "lease_plan_snapshot", length = 200)
        private String leasePlanSnapshot;  // For audit trail
    }

    /**
     * Embedded value object for financial summary
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialSummary {
        @Column(name = "total_revenue", precision = 10, scale = 2)
        private BigDecimal totalRevenue;

        @Column(name = "total_expenses", precision = 10, scale = 2)
        private BigDecimal totalExpenses;

        @Column(name = "net_revenue", precision = 10, scale = 2)
        private BigDecimal netRevenue;

        @Column(name = "total_lease_amount", precision = 10, scale = 2)
        private BigDecimal totalLeaseAmount;

        @Column(name = "owner_earnings", precision = 10, scale = 2)
        private BigDecimal ownerEarnings;

        @Column(name = "driver_earnings", precision = 10, scale = 2)
        private BigDecimal driverEarnings;
    }
}
