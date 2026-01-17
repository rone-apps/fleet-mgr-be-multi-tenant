package com.taxi.domain.shift.model;

import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entity representing a segment of a shift driven by a specific driver
 * Enables tracking of handovers where multiple drivers operate the same shift
 * 
 * Business Rule: Lease is split proportionally based on miles driven
 */
@Entity
@Table(name = "driver_segment", 
       indexes = {
           @Index(name = "idx_segment_log", columnList = "shift_log_id, sequence_number"),
           @Index(name = "idx_segment_driver", columnList = "driver_id, start_time")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shiftLog", "driver"})
@EqualsAndHashCode(of = "id")
public class DriverSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_log_id", nullable = false)
    private ShiftLog shiftLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;  // 1, 2, 3... for ordering segments

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "start_meter_reading", precision = 10, scale = 2)
    private BigDecimal startMeterReading;

    @Column(name = "end_meter_reading", precision = 10, scale = 2)
    private BigDecimal endMeterReading;

    @Column(name = "segment_miles", precision = 10, scale = 2)
    private BigDecimal segmentMiles;

    @Column(name = "segment_lease_share", precision = 10, scale = 2)
    private BigDecimal segmentLeaseShare;

    @Column(name = "segment_revenue", precision = 10, scale = 2)
    private BigDecimal segmentRevenue;

    @Column(name = "segment_expenses", precision = 10, scale = 2)
    private BigDecimal segmentExpenses;

    @Column(name = "segment_net_earnings", precision = 10, scale = 2)
    private BigDecimal segmentNetEarnings;

    @Column(name = "notes", length = 500)
    private String notes;

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
     * Business logic: Calculate segment miles from meter readings
     */
    public void calculateSegmentMiles() {
        if (startMeterReading != null && endMeterReading != null) {
            this.segmentMiles = endMeterReading.subtract(startMeterReading);
        }
    }

    /**
     * Business logic: Calculate this segment's proportional lease share
     * @param totalMiles Total miles for the entire shift
     * @param totalLease Total lease amount for the entire shift
     */
    public void calculateLeaseShare(BigDecimal totalMiles, BigDecimal totalLease) {
        if (segmentMiles == null || totalMiles == null || totalMiles.compareTo(BigDecimal.ZERO) == 0) {
            this.segmentLeaseShare = BigDecimal.ZERO;
            return;
        }

        // Proportional calculation: (segmentMiles / totalMiles) × totalLease
        BigDecimal proportion = segmentMiles.divide(totalMiles, 4, RoundingMode.HALF_UP);
        this.segmentLeaseShare = proportion.multiply(totalLease).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Business logic: Calculate segment net earnings
     * Net Earnings = Segment Revenue - Segment Expenses - Segment Lease Share
     */
    public void calculateNetEarnings() {
        BigDecimal revenue = segmentRevenue != null ? segmentRevenue : BigDecimal.ZERO;
        BigDecimal expenses = segmentExpenses != null ? segmentExpenses : BigDecimal.ZERO;
        BigDecimal lease = segmentLeaseShare != null ? segmentLeaseShare : BigDecimal.ZERO;

        this.segmentNetEarnings = revenue.subtract(expenses).subtract(lease);
    }

    /**
     * Business logic: End this segment (when driver hands over or shift completes)
     */
    public void endSegment(LocalDateTime endTime, BigDecimal endMeterReading) {
        this.endTime = endTime;
        this.endMeterReading = endMeterReading;
        calculateSegmentMiles();
    }

    /**
     * Business logic: Check if this segment is currently active
     */
    public boolean isActive() {
        return endTime == null;
    }

    /**
     * Business logic: Get driver name for display
     */
    public String getDriverName() {
        return driver != null 
            ? driver.getFirstName() + " " + driver.getLastName() 
            : "Unknown";
    }
}
