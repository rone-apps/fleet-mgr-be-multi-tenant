package com.taxi.web.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO for legacy customer charge display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacyCustomerChargeDTO {
    private Long id;
    private Double amount;
    private LocalDate date;
    private Double payment;
    private Long cabId;
    private String notes;
    private String type;

    // Customer info
    private Long customerDbId;
    private String customerId;
    private String customerName;

    // Driver info
    private Long driverDbId;
    private String driverNumber;
    private String driverName;
}
