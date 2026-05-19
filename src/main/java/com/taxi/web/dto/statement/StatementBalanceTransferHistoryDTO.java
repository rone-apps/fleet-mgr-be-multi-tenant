package com.taxi.web.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for statement balance transfer history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceTransferHistoryDTO {

    private Long id;
    private Long transferId;
    private String transferNumber;

    private Long sourceStatementId;
    private Long targetStatementId;

    private BigDecimal transferAmount;
    private LocalDate appliedPeriodFrom;
    private LocalDate appliedPeriodTo;

    private LocalDateTime appliedAt;
    private Long appliedBy;
    private String description;

    // Reversal tracking
    private Boolean isReversed;
    private LocalDateTime reversedAt;
    private Long reversedBy;
    private String reversalReason;
}
