package com.taxi.domain.expense.model;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.lease.model.LeasePlan;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.shift.model.ShiftType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks actual lease charges incurred by drivers
 * Based on lease plans and rates
 */
@Entity
@Table(name = "lease_expense")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The driver who incurred this lease charge
     */
    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    /**
     * The cab that was leased
     */
    @Column(name = "cab_id", nullable = false)
    private Long cabId;

    /**
     * Date of the lease charge
     */
    @Column(name = "lease_date", nullable = false)
    private LocalDate leaseDate;

    /**
     * Day of week for the lease
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /**
     * Shift type (MORNING/EVENING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftType shiftType;

    /**
     * Type of cab (SEDAN/HANDICAP_VAN)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cab_type", nullable = false)
    private CabType cabType;

    /**
     * Whether cab has airport license
     */
    @Column(name = "has_airport_license", nullable = false)
    private Boolean hasAirportLicense;

    /**
     * The lease plan that was active
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LeasePlan leasePlan;

    /**
     * The specific lease rate applied
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_id", nullable = false)
    private LeaseRate leaseRate;

    /**
     * Base lease amount charged
     */
    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Miles driven (if applicable)
     */
    @Column(name = "miles_driven", precision = 10, scale = 2)
    private BigDecimal milesDriven;

    /**
     * Mileage charge (miles * mileage_rate)
     */
    @Column(name = "mileage_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal mileageAmount = BigDecimal.ZERO;

    /**
     * Total lease charge (base + mileage)
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Reference to shift if this was calculated from a shift
     */
    @Column(name = "shift_id")
    private Long shiftId;

    /**
     * Additional notes
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Whether this lease has been paid
     */
    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    /**
     * Date paid (if paid)
     */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Calculate day of week from lease date if not set
        if (dayOfWeek == null && leaseDate != null) {
            dayOfWeek = leaseDate.getDayOfWeek();
        }
        
        // Calculate total amount
        calculateTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotal();
    }

    /**
     * Calculate total amount (base + mileage)
     */
    public void calculateTotal() {
        BigDecimal base = baseAmount != null ? baseAmount : BigDecimal.ZERO;
        BigDecimal mileage = mileageAmount != null ? mileageAmount : BigDecimal.ZERO;
        totalAmount = base.add(mileage);
    }
}
