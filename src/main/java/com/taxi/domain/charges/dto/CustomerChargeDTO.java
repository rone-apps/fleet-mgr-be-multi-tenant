package com.taxi.domain.charges.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Normalized DTO for customer charges from any source.
 * Adapts both legacy_customer_charge and AccountCharge entities to a common format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerChargeDTO {

    // Core identifiers
    private Long id;
    private LocalDate chargeDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // Customer information
    private String customerName;
    private String accountId;

    // Trip details
    private String jobCode;
    private String pickupAddress;
    private String dropoffAddress;
    private String passengerName;

    // Vehicle and driver
    private String cabNumber;
    private String driverNumber;
    private String driverName;

    // Financial amounts
    private BigDecimal fareAmount;
    private BigDecimal tipAmount;
    private BigDecimal totalAmount;

    // Payment tracking
    private Boolean isPaid;
    private LocalDate paidDate;
    private String invoiceNumber;

    // Metadata
    private String sourceSystem;  // "LEGACY" or "MODERN"
    private String notes;
}
