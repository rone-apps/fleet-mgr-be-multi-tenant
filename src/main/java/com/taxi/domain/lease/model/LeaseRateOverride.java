package com.taxi.domain.lease.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Custom lease rate overrides for specific cab/shift combinations
 * 
 * Allows cab owners to set custom lease rates that override the default rates
 * Can be configured by:
 * - Cab number (specific cab or null for all owner's cabs)
 * - Shift type (DAY, NIGHT, or null for both)
 * - Day of week (MONDAY, TUESDAY, etc., or null for all days)
 * - Date range (start date required, end date null means ongoing)
 * 
 * Example use cases:
 * 1. "Cab 1, DAY shift, MONDAY-THURSDAY = $45, ongoing"
 * 2. "Cab 1, DAY shift, FRIDAY-SUNDAY = $65, ongoing"
 * 3. "All my cabs, NIGHT shift, all days = $75, Dec 1-31, 2025"
 * 4. "Cab 2, both shifts, SATURDAY = $80, ongoing"
 */
@Entity
@Table(
    name = "lease_rate_overrides",
    indexes = {
        @Index(name = "idx_owner_cab", columnList = "owner_driver_number, cab_number"),
        @Index(name = "idx_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_active", columnList = "is_active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseRateOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner who sets this custom rate
     * Required - must be a driver with isOwner=true
     */
    @Column(name = "owner_driver_number", nullable = false, length = 50)
    private String ownerDriverNumber;

    /**
     * Specific cab number (nullable)
     * - If null: applies to all cabs owned by this owner
     * - If set: applies only to this specific cab
     */
    @Column(name = "cab_number", length = 50)
    private String cabNumber;

    /**
     * Shift type (nullable)
     * - "DAY": applies to day shifts only
     * - "NIGHT": applies to night shifts only
     * - null: applies to both day and night shifts
     */
    @Column(name = "shift_type", length = 20)
    private String shiftType; // "DAY", "NIGHT", or null for both

    /**
     * Day of week (nullable)
     * - "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
     * - null: applies to all days of the week
     */
    @Column(name = "day_of_week", length = 20)
    private String dayOfWeek; // "MONDAY", "TUESDAY", etc., or null for all days

    /**
     * Custom lease rate
     * This amount overrides the default lease rate when conditions match
     */
    @Column(name = "lease_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal leaseRate;

    /**
     * Start date (required)
     * Override becomes active on this date
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * End date (nullable)
     * - If null: ongoing indefinitely
     * - If set: override expires after this date
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Active flag
     * Allows temporarily disabling without deleting
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Priority for conflict resolution
     * When multiple overrides match, highest priority wins
     * Higher number = higher priority
     * 
     * Recommended priority levels:
     * - 100: Specific cab + specific shift + specific day
     * - 75: Specific cab + specific shift + all days
     * - 50: Specific cab + all shifts + specific day
     * - 25: All cabs + specific shift + specific day
     * - 10: Broad overrides
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 10;

    /**
     * Optional notes/description
     */
    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /**
     * Calculate priority automatically based on specificity
     * More specific overrides get higher priority
     */
    public void calculatePriority() {
        int calculatedPriority = 0;
        
        // Specific cab: +50 points
        if (cabNumber != null && !cabNumber.isEmpty()) {
            calculatedPriority += 50;
        }
        
        // Specific shift type: +30 points
        if (shiftType != null && !shiftType.isEmpty()) {
            calculatedPriority += 30;
        }
        
        // Specific day of week: +20 points
        if (dayOfWeek != null && !dayOfWeek.isEmpty()) {
            calculatedPriority += 20;
        }
        
        this.priority = calculatedPriority;
    }

    /**
     * Check if this override is currently active for a given date
     */
    public boolean isActiveOn(LocalDate date) {
        if (!isActive) {
            return false;
        }
        
        if (date.isBefore(startDate)) {
            return false;
        }
        
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if this override matches the given criteria
     */
    public boolean matches(String cabNum, String shift, String dayOfWeekStr) {
        // Check cab number match (null means applies to all cabs)
        if (cabNumber != null && !cabNumber.isEmpty() && !cabNumber.equals(cabNum)) {
            return false;
        }
        
        // Check shift type match (null means applies to all shifts)
        if (shiftType != null && !shiftType.isEmpty() && !shiftType.equalsIgnoreCase(shift)) {
            return false;
        }
        
        // Check day of week match (null means applies to all days)
        if (dayOfWeek != null && !dayOfWeek.isEmpty() && !dayOfWeek.equalsIgnoreCase(dayOfWeekStr)) {
            return false;
        }
        
        return true;
    }
}
