package com.taxi.web.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for statement balance transfer data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceTransferDTO {

    private Long id;
    private String transferNumber;

    // Source
    private Long sourcePersonId;
    private String sourcePersonType;
    private String sourcePersonName;

    // Target
    private Long targetPersonId;
    private String targetPersonType;
    private String targetPersonName;

    // Configuration
    private String transferType;
    private String balanceDirection;

    // Amounts
    private BigDecimal transferAmount;
    private BigDecimal transferredAmount;
    private BigDecimal remainingAmount;

    // Date range
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate statementPeriodFrom;
    private LocalDate statementPeriodTo;

    // Status
    private String status;

    // Metadata
    private String description;
    private String notes;
    private String reason;

    // Audit
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private String cancellationReason;
}
