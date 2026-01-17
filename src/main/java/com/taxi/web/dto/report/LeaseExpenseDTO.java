package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for a single lease expense item
 * Represents a shift where a driver worked a shift owned by someone else
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseExpenseDTO {
    
    // Shift identification
    private Long shiftId;
    private LocalDate shiftDate;
    private LocalDateTime logonTime;
    private LocalDateTime logoffTime;
    
    // Shift details
    private String cabNumber;
    private String shiftType;          // "DAY" or "NIGHT"
    private BigDecimal totalHours;
    
    // Owner information (who receives the lease payment)
    private String ownerDriverNumber;
    private String ownerDriverName;
    
    // Lease calculation
    private BigDecimal baseRate;       // Fixed base rate
    private BigDecimal miles;          // Miles driven
    private BigDecimal mileageRate;    // Per-mile rate
    private BigDecimal mileageLease;   // miles × mileageRate
    private BigDecimal totalLease;     // baseRate + mileageLease
    
    // Additional context
    private String cabType;            // SEDAN, VAN, etc.
    private Boolean hasAirportLicense;
}