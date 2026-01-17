package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for individual credit card transaction in report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardRevenueDTO {
    
    private Long transactionId;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    
    // Transaction details
    private String authorizationCode;
    private String terminalId;
    private String merchantId;
    private String batchNumber;
    
    // Card info (masked)
    private String cardType;
    private String cardLastFour;
    
    // Business context
    private String cabNumber;
    private String driverNumber;
    private String driverName;
    private String jobId;
    
    // Financial
    private BigDecimal amount;
    private BigDecimal tipAmount;
    private BigDecimal totalAmount;
    private BigDecimal processingFee;
    private BigDecimal netAmount;
    
    // Status
    private String transactionStatus;
    private Boolean isSettled;
    private LocalDate settlementDate;
    private Boolean isRefunded;
    private BigDecimal refundAmount;
    
    private String receiptNumber;
    private String customerName;
    private String notes;
}
