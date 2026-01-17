package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for individual charge revenue item in report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargesRevenueDTO {
    
    private Long chargeId;
    private LocalDate tripDate;
    private LocalTime startTime;
    private LocalTime endTime;
    
    // Account information
    private String accountId;
    private String customerName;
    private String subAccount;
    
    // Trip details
    private String jobCode;
    private String pickupAddress;
    private String dropoffAddress;
    private String passengerName;
    
    // Cab and driver
    private String cabNumber;
    private String driverNumber;
    private String driverName;
    
    // Financial
    private BigDecimal fareAmount;
    private BigDecimal tipAmount;
    private BigDecimal totalAmount;  // fare + tip
    
    // Payment status
    private Boolean isPaid;
    private LocalDate paidDate;
    private String invoiceNumber;
    
    private String notes;
}