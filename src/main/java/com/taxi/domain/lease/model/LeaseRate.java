package com.taxi.domain.lease.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.shift.model.ShiftType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * Entity representing a specific lease rate configuration
 * Rate is determined by: CabType + AirportLicense + ShiftType + DayOfWeek
 * 
 * Total Lease = Base Rate + (Miles Driven × Mileage Rate)
 * 
 * IMPORTANT: Rates are IMMUTABLE once created (cannot edit)
 * To change rates: create new plan with new rates
 */
@Entity
@Table(name = "lease_rate", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"plan_id", "cab_type", "has_airport_license", "shift_type", "day_of_week"}
       ),
       indexes = {
           @Index(name = "idx_rate_lookup", columnList = "cab_type, has_airport_license, shift_type, day_of_week")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "leasePlan")
@EqualsAndHashCode(of = "id")
public class LeaseRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LeasePlan leasePlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "cab_type", nullable = false, length = 20)
    private CabType cabType;

    @Column(name = "has_airport_license", nullable = false)
    private boolean hasAirportLicense;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "mileage_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal mileageRate;

    @Column(name = "notes", length = 200)
    private String notes;

    /**
     * Business logic: Calculate total lease for given mileage
     */
    public BigDecimal calculateTotalLease(BigDecimal milesDriven) {
        if (milesDriven == null || milesDriven.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Miles driven cannot be null or negative");
        }
        BigDecimal mileageCharge = mileageRate.multiply(milesDriven);
        return baseRate.add(mileageCharge);
    }

    /**
     * Business logic: Check if this rate matches the given criteria
     */
    public boolean matches(CabType cabType, boolean hasAirportLicense, 
                          ShiftType shiftType, DayOfWeek dayOfWeek) {
        return this.cabType == cabType
                && this.hasAirportLicense == hasAirportLicense
                && this.shiftType == shiftType
                && this.dayOfWeek == dayOfWeek;
    }
}
