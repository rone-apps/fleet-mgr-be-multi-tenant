package com.taxi.domain.shift.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a driver shift with automatic day/night shift calculation.
 * 
 * Business Rules:
 * - Logon between 00:00-11:59 = DAY shift
 * - Logon between 12:00-23:59 = NIGHT shift
 * - Up to 12 hours = 1 shift of primary type
 * - 12-15 hours = 1 primary + 0.25 secondary
 * - 15-18 hours = 1 primary + 0.5 secondary
 * - 18+ hours = 1 primary + 1 full secondary
 */
@Entity
@Table(name = "driver_shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_number", length = 50, nullable = false)
    private String driverNumber;  // Matches driver.driver_number (VARCHAR)
    
    @Column(name = "driver_username", length = 50)
    private String driverUsername; // From TaxiCaller
    
    @Column(name = "driver_first_name", length = 100)
    private String driverFirstName; // From TaxiCaller
    
    @Column(name = "driver_last_name", length = 100)
    private String driverLastName; // From TaxiCaller

    @Column(name = "cab_number", length = 50, nullable = false)
    private String cabNumber;

    @Column(name = "logon_time", nullable = false)
    private LocalDateTime logonTime;

    @Column(name = "logoff_time")
    private LocalDateTime logoffTime;

    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "primary_shift_type", length = 10)
    private String primaryShiftType; // DAY or NIGHT

    @Column(name = "primary_shift_count", precision = 3, scale = 2)
    private BigDecimal primaryShiftCount = BigDecimal.ZERO;

    @Column(name = "secondary_shift_type", length = 10)
    private String secondaryShiftType; // DAY or NIGHT (opposite of primary)

    @Column(name = "secondary_shift_count", precision = 3, scale = 2)
    private BigDecimal secondaryShiftCount = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, CANCELLED

    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    @Column(name = "total_revenue", precision = 10, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Transient fields for easier access (not stored in DB)
    @Transient
    private String driverName;

    // Helper methods
    public BigDecimal getTotalDayShifts() {
        BigDecimal dayShifts = BigDecimal.ZERO;
        if ("DAY".equals(primaryShiftType)) {
            dayShifts = dayShifts.add(primaryShiftCount);
        }
        if ("DAY".equals(secondaryShiftType) && secondaryShiftCount != null) {
            dayShifts = dayShifts.add(secondaryShiftCount);
        }
        return dayShifts;
    }

    public BigDecimal getTotalNightShifts() {
        BigDecimal nightShifts = BigDecimal.ZERO;
        if ("NIGHT".equals(primaryShiftType)) {
            nightShifts = nightShifts.add(primaryShiftCount);
        }
        if ("NIGHT".equals(secondaryShiftType) && secondaryShiftCount != null) {
            nightShifts = nightShifts.add(secondaryShiftCount);
        }
        return nightShifts;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}