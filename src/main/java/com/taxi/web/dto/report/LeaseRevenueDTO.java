package com.taxi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for lease revenue report line items
 * Represents lease owed to a cab owner for a specific shift
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseRevenueDTO {
    
    private Long shiftId;
    private LocalDate shiftDate;
    private LocalDateTime logonTime;
    private LocalDateTime logoffTime;
    
    // Driver who drove the cab
    private String driverNumber;
    private String driverName;
    
    // Cab that was driven
    private String cabNumber;
    
    // Shift details
    private String shiftType; // DAY or NIGHT
    private BigDecimal totalHours;
    
    // Lease calculation components
    private BigDecimal baseRate;
    private BigDecimal miles;
    private BigDecimal mileageRate;
    private BigDecimal mileageLease;
    private BigDecimal totalLease;
    
    // Additional context
    private String cabType;
    private Boolean hasAirportLicense;
    
    /**
     * Helper method to calculate mileage lease from miles and rate
     */
    public void calculateMileageLease() {
        if (miles != null && mileageRate != null) {
            this.mileageLease = miles.multiply(mileageRate);
        } else {
            this.mileageLease = BigDecimal.ZERO;
        }
    }
    
    /**
     * Helper method to calculate total lease
     */
    public void calculateTotalLease() {
        BigDecimal base = baseRate != null ? baseRate : BigDecimal.ZERO;
        BigDecimal mileage = mileageLease != null ? mileageLease : BigDecimal.ZERO;
        this.totalLease = base.add(mileage);
    }
}
