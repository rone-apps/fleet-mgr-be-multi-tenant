package com.taxi.domain.csvuploader;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for credit card transaction CSV upload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardTransactionUploadDTO {
    
    private Long id;
    
    // CSV fields
    private String transactionId;
    private String authorizationCode;
    private String merchantId;
    private String terminalId;
    private String siteNumber;
    private String deviceNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime transactionTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate settlementDate;
    
    private String cardType;
    private String cardholderNumber;
    private String cardLastFour;
    private String cardBrand;
    private String cabNumber;
    private String driverNumber;
    private String driverName;
    private String jobId;
    
    private BigDecimal amount;
    private BigDecimal tipAmount;
    private BigDecimal processingFee;
    
    private String transactionType;
    private String transactionStatus;
    private Boolean isSettled;
    private Boolean isRefunded;
    private BigDecimal refundAmount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate refundDate;
    
    private String batchNumber;
    private String invoiceNumber;
    private String referenceNumber;
    private String merchantReferenceNumber;
    private String customerName;
    private String receiptNumber;
    private String potCode;
    private String storeNumber;
    private String clerkId;
    private String currency;
    private String notes;
    
    // Metadata
    private String uploadBatchId;
    private String uploadFilename;
    
    // Validation status
    private boolean valid;
    private String validationMessage;
    private boolean cabLookupSuccess;
    private boolean driverLookupSuccess;
    private String lookupMessage;
    
    // Computed fields
    private BigDecimal totalAmount;
    private BigDecimal netAmount;
}
